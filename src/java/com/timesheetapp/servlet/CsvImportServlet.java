package com.timesheetapp.servlet;

import com.timesheetapp.util.DatabaseUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/import-csv")
public class CsvImportServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CsvImportServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if the request contains file data (multipart/form-data)
        if (!ServletFileUpload.isMultipartContent(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Form must be multipart/form-data.\"}");
            return;
        }

        try {
            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request to get the file items
            List<FileItem> items = upload.parseRequest(request);

            InputStream csvInputStream = null;

            // Iterate over form fields and file items
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    // Get the uploaded file as an InputStream
                    csvInputStream = item.getInputStream();
                }
            }

            // If the CSV file was uploaded, process it
            if (csvInputStream != null) {
                importCsvData(csvInputStream, response);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\": \"No file uploaded.\"}");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to import CSV data: " + e.getMessage() + "\"}");
        }
    }

    private void importCsvData(InputStream inputStream, HttpServletResponse response) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        BufferedReader br = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int batchCount = 0;
        int totalRecords = 0;
        int skippedRecords = 0;

        // Set to track duplicate epic-feature combinations within the same CSV file
        Set<String> processedCombinations = new HashSet<String>();

        try {
            conn = new DatabaseUtils().createConnection();
            String query = "INSERT INTO TIMESHEETDB.timesheet_entries (epic, feature, application, ur_description, release_date, change_no, effort, remarks, workscope, boxPath) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(query);

            // UTF-8 encoding
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            StringBuilder recordBuffer = new StringBuilder();
            boolean insideQuotes = false;

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

                // Skip rows that are blank in all columns
                if (values == null || isRowEmpty(values)) {
                    skippedRecords++;
                    continue;
                }

                String epic = values.length > 0 ? values[0].trim() : null;
                String feature = values.length > 1 ? values[1].trim() : null;

                // Only skip the row if BOTH epic and feature are non-null and non-blank
                if (epic != null && !epic.isEmpty() && feature != null && !feature.isEmpty()) {
                    String combination = epic + "|" + feature;

                    // Check for duplicates within the same CSV file
                    if (processedCombinations.contains(combination)) {
                        LOGGER.log(Level.INFO, "Skipping duplicate record in CSV with epic: " + epic + " and feature: " + feature);
                        skippedRecords++;
                        continue;
                    }

                    // Check if the combination already exists in the database
                    if (isDuplicateEpicAndFeature(conn, epic, feature)) {
                        LOGGER.log(Level.INFO, "Skipping duplicate record in database with epic: " + epic + " and feature: " + feature);
                        skippedRecords++;
                        continue;
                    }

                    // Add the combination to the set to avoid duplicates within the same CSV file
                    processedCombinations.add(combination);
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
                ps.setString(1, escapeHtml(epic));
                ps.setString(2, escapeHtml(feature));
                ps.setString(3, escapeHtml(values.length > 2 ? values[2].trim() : null)); // application
                ps.setString(4, escapeHtml(values.length > 3 ? values[3].trim() : null)); // ur_description
                ps.setDate(5, releaseDate); // release_date
                ps.setString(6, escapeHtml(values.length > 5 ? values[5].trim() : null)); // change_no
                ps.setString(7, escapeHtml(values.length > 6 ? values[6].trim() : null)); // effort
                ps.setString(8, escapeHtml(values.length > 7 ? values[7].trim() : null)); // remarks
                ps.setString(9, escapeHtml(values.length > 8 ? values[8].trim() : null)); // workscope
                ps.setString(10, escapeHtml(values.length > 9 ? values[9].trim() : null)); // boxPath

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

    // Check if both epic and feature already exist in the database
    private boolean isDuplicateEpicAndFeature(Connection conn, String epic, String feature) throws SQLException {
        String query = "SELECT COUNT(*) FROM TIMESHEETDB.timesheet_entries WHERE epic = ? AND feature = ?";
        PreparedStatement checkStmt = null;
        ResultSet rs = null;
        try {
            checkStmt = conn.prepareStatement(query);
            checkStmt.setString(1, epic);
            checkStmt.setString(2, feature);
            rs = checkStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // If count > 0, it's a duplicate
            }
        } finally {
            if (rs != null) rs.close();
            if (checkStmt != null) checkStmt.close();
        }
        return false;
    }

    // Check if the row is completely empty (all columns are blank or null)
    private boolean isRowEmpty(String[] values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return false; // The row is not empty if any value is non-blank
            }
        }
        return true; // The row is empty if all values are blank or null
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
