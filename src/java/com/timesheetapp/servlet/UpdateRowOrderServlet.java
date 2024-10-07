package com.timesheetapp.servlet;

import com.timesheetapp.util.DatabaseUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/updateRowOrder")
public class UpdateRowOrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");  
        response.setCharacterEncoding("UTF-8");

        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            jsonBuffer.append(line);
        }

        String json = jsonBuffer.toString();

        // Parse the JSON string into a List of RowOrder objects
        List<RowOrder> rowOrderList = parseJsonToRowOrderList(json);

        if (rowOrderList == null || rowOrderList.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid data format.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = new DatabaseUtils().createConnection();
            String updateQuery = "UPDATE TIMESHEETDB.timesheet_entries SET display_order = ? WHERE id = ?";
            ps = conn.prepareStatement(updateQuery);
            for (RowOrder rowOrder : rowOrderList) {
                ps.setInt(1, rowOrder.getDisplayOrder());
                ps.setInt(2, rowOrder.getId());
                ps.addBatch();
            }
            ps.executeBatch();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\": true}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception ex) {
            Logger.getLogger(UpdateRowOrderServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<RowOrder> parseJsonToRowOrderList(String json) {
        List<RowOrder> rowOrderList = new ArrayList<RowOrder>();

        try {
            // Remove leading and trailing brackets (if present)
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }

            String[] rowOrderEntries = json.split("},\\{");

            for (String entry : rowOrderEntries) {
                entry = entry.replace("{", "").replace("}", "");  // Remove remaining curly braces
                RowOrder rowOrder = new RowOrder();

                String[] keyValuePairs = entry.split(",");
                for (String keyValuePair : keyValuePairs) {
                    String[] keyValue = keyValuePair.split(":");
                    String key = keyValue[0].replace("\"", "").trim();  // Remove quotes and trim spaces
                    String value = keyValue[1].trim();

                    if (key.equals("id")) {
                        rowOrder.setId(Integer.parseInt(value));
                    } else if (key.equals("displayOrder")) {
                        rowOrder.setDisplayOrder(Integer.parseInt(value));
                    }
                }

                rowOrderList.add(rowOrder);
            }
        } catch (Exception e) {
            // Log and handle any parsing errors
            e.printStackTrace();
            return null;
        }

        return rowOrderList;
    }
}

class RowOrder {
    private int id;
    private int displayOrder;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
