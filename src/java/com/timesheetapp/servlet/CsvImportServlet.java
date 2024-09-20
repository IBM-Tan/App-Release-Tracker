package com.timesheetapp.servlet;

import com.timesheetapp.util.DatabaseUtils;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/import-csv")
public class CsvImportServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CsvImportServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filePath = request.getParameter("filePath");

        if (filePath == null || filePath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"File path is required.\"}");
            return;
        }

        try {
            importCsvData(filePath, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to import CSV data: " + e.getMessage() + "\"}");
        }
    }

    private void importCsvData(String filePath, HttpServletResponse response) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        BufferedReader br = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int batchCount = 0;
        int totalRecords = 0;
        int skippedRecords = 0;

        try {
            conn = new DatabaseUtils().createConnection();
            String query = "INSERT INTO TIMESHEETDB.timesheet_entries (epic, feature, application, ur_description, release_date, change_no, effort, remarks, workscope, boxPath) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(query);

            //UTF-8 encoding
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
            String line;
            StringBuilder recordBuffer = new StringBuilder();  // For handling multi-line fields
            boolean insideQuotes = false;  // Track multi-line field
            List<String> parsedFields = new ArrayList<String>();

            while ((line = br.readLine()) != null) {
                recordBuffer.append(line).append("\n");

                int quoteCount = countQuotes(line);
                if (quoteCount % 2 != 0) {
                    insideQuotes = !insideQuotes;
                }

                // If still inside quotes, continue accumulating lines
                if (insideQuotes) {
                    continue;
                }

                totalRecords++;
                String fullRecord = recordBuffer.toString();
                recordBuffer.setLength(0); // Clear buffer

                String[] values = parseCsvLine(fullRecord);
                if (values == null) {
                    skippedRecords++;
                    continue;
                }

                // Process date conversion
                java.sql.Date releaseDate = null;
                try {
                    if (values.length > 4 && !values[4].trim().isEmpty()) {
                        releaseDate = new java.sql.Date(dateFormat.parse(values[4].trim()).getTime());
                    }
                } catch (ParseException e) {
                    LOGGER.log(Level.WARNING, "Invalid date format for release_date: " + values[4] + " - Skipping this record.");
                    skippedRecords++;
                    continue;
                }

                // Escape and set each value, or null if missing
                ps.setString(1, escapeHtml(values.length > 0 && !values[0].trim().isEmpty() ? values[0].trim() : null)); // epic
                ps.setString(2, escapeHtml(values.length > 1 && !values[1].trim().isEmpty() ? values[1].trim() : null)); // feature
                ps.setString(3, escapeHtml(values.length > 2 && !values[2].trim().isEmpty() ? values[2].trim() : null)); // application
                ps.setString(4, escapeHtml(values.length > 3 && !values[3].trim().isEmpty() ? values[3].trim() : null)); // ur_description
                ps.setDate(5, releaseDate); // release_date
                ps.setString(6, escapeHtml(values.length > 5 && !values[5].trim().isEmpty() ? values[5].trim() : null)); // change_no
                ps.setString(7, escapeHtml(values.length > 6 && !values[6].trim().isEmpty() ? values[6].trim() : null)); // effort
                ps.setString(8, escapeHtml(values.length > 7 && !values[7].trim().isEmpty() ? values[7].trim() : null)); // remarks
                ps.setString(9, escapeHtml(values.length > 8 && !values[8].trim().isEmpty() ? values[8].trim() : null)); // workscope
                ps.setString(10, escapeHtml(values.length > 9 && !values[9].trim().isEmpty() ? values[9].trim() : null)); // boxPath

                ps.addBatch();
                batchCount++;
            }

            int[] result = ps.executeBatch();
            LOGGER.info("Number of records inserted: " + result.length);
            LOGGER.info("Total records processed: " + totalRecords);
            LOGGER.info("Records skipped: " + skippedRecords);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("{\"success\": \"Successfully imported " + result.length + " records. Total processed: " + totalRecords + ", Skipped: " + skippedRecords + "\"}");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error while inserting data from CSV", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to insert data from CSV. SQL Error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while processing CSV file", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to import CSV data. Error: " + e.getMessage() + "\"}");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close BufferedReader", e);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close PreparedStatement", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close Connection", e);
                }
            }
        }

        LOGGER.info("Total number of records processed: " + batchCount);
    }

    // Parse CSV line with possible quoted fields and commas inside fields
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                insideQuotes = !insideQuotes; // Toggle state when encountering a quote
            } else if (c == ',' && !insideQuotes) {
                // Split at commas only if not inside quotes
                values.add(currentField.toString().trim());
                currentField.setLength(0); // Reset the buffer for the next field
            } else {
                currentField.append(c);
            }
        }
        values.add(currentField.toString().trim()); // Add the last field

        return values.toArray(new String[0]);
    }

    // Escape special characters
    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("@", "&#64;");
    }

    // Merge lines for multi-line fields
    private String mergeLines(List<String> lines) {
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            merged.append(lines.get(i));
            if (i < lines.size() - 1) {
                merged.append("\n"); // Add newline except for the last element
            }
        }
        return merged.toString();
    }

    // Count the number of quotes in a line to handle multi-line fields
    private int countQuotes(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                count++;
            }
        }
        return count;
    }
}
