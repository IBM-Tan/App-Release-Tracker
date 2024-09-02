package com.timesheetapp.servlet;


import com.timesheetapp.util.DatabaseUtils;
import java.io.BufferedReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/timesheet/*")
public class TimeSheetServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TimeSheetServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                getAllTimesheetEntries(response, out);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve all timesheet entries", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\": \"Failed to retrieve timesheet data.\"}");
            }
        } else {
            String[] splits = pathInfo.split("/");
            if (splits.length > 1) {
                String entryId = splits[1];
                try {
                    getTimeSheetEntryById(entryId, response, out);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to retrieve timesheet entry by ID", ex);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\": \"Failed to retrieve timesheet entry.\"}");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Invalid URL.\"}");
            }
        }
    }

    private void getAllTimesheetEntries(HttpServletResponse response, PrintWriter out) throws Exception {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
        conn = new DatabaseUtils().createConnection();
        String query = "SELECT * FROM TIMESHEETDB.timesheet_entries";
        ps = conn.prepareStatement(query);
        rs = ps.executeQuery();
        StringBuilder json = new StringBuilder("[");
        while (rs.next()) {
            json.append("{")
                .append("\"id\":").append(rs.getInt("id")).append(",")
                .append("\"epic\":\"").append(rs.getString("epic")).append("\",")
                .append("\"feature\":\"").append(rs.getString("feature")).append("\",")
                .append("\"application\":\"").append(rs.getString("application")).append("\",")
                .append("\"ur_description\":\"").append(rs.getString("ur_description")).append("\",")
                .append("\"release\":\"").append(rs.getString("release_date")).append("\",")
                .append("\"change_no\":\"").append(rs.getString("change_no")).append("\",")
                .append("\"remarks\":\"").append(rs.getString("remarks")).append("\",")
                .append("\"workscope\":\"").append(rs.getString("workscope")).append("\",")
                .append("\"effort\":\"").append(rs.getString("effort")).append("\",")
                .append("\"boxPath\":\"").append(rs.getString("boxPath")).append("\"")
                .append("},");
        }
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        json.append("]");
        out.print(json.toString());
        out.flush();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "SQL Error while retrieving all timesheet entries", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print("{\"error\": \"Failed to retrieve timesheet data.\"}");
    } finally {
        closeResources(rs, ps, conn);
    }
}

private void getTimeSheetEntryById(String entryId, HttpServletResponse response, PrintWriter out) throws Exception {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
        conn = new DatabaseUtils().createConnection();
        String query = "SELECT * FROM TIMESHEETDB.timesheet_entries WHERE id = ?";
        ps = conn.prepareStatement(query);
        ps.setInt(1, Integer.parseInt(entryId));
        rs = ps.executeQuery();

        if (rs.next()) {
            String json = "{"
                + "\"id\":" + rs.getInt("id") + ","
                + "\"epic\":\"" + rs.getString("epic") + "\","
                + "\"feature\":\"" + rs.getString("feature") + "\","
                + "\"application\":\"" + rs.getString("application") + "\","
                + "\"ur_description\":\"" + rs.getString("ur_description") + "\","
                + "\"release\":\"" + rs.getString("release_date") + "\","
                + "\"change_no\":\"" + rs.getString("change_no") + "\","
                + "\"remarks\":\"" + rs.getString("remarks") + "\","
                + "\"workscope\":\"" + rs.getString("workscope") + "\","
                + "\"effort\":\"" + rs.getString("effort") + "\","
                + "\"boxPath\":\"" + rs.getString("boxPath") + "\""
                + "}";
            out.print(json);
            out.flush();
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\": \"Timesheet entry not found.\"}");
        }
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "SQL Error while retrieving timesheet entry by ID", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print("{\"error\": \"Failed to retrieve timesheet entry.\"}");
    } finally {
        closeResources(rs, ps, conn);
    }
}


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            addTimeSheetEntry(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add timesheet entry", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to add timesheet entry.\"}");
        }
    }

    private void addTimeSheetEntry(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Connection conn = null;
    PreparedStatement ps = null;
    try {
        conn = new DatabaseUtils().createConnection();
        String query = "INSERT INTO TIMESHEETDB.timesheet_entries (epic, feature, application, ur_description, release_date, change_no, remarks, workscope, effort, boxPath) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ps = conn.prepareStatement(query);
        ps.setString(1, request.getParameter("epic"));
        ps.setString(2, request.getParameter("feature"));
        ps.setString(3, request.getParameter("application"));
        ps.setString(4, request.getParameter("ur_description"));
        ps.setString(5, request.getParameter("release_date"));
        ps.setString(6, request.getParameter("change_no"));
        ps.setString(7, request.getParameter("remarks"));
        ps.setString(8, request.getParameter("workscope"));
        ps.setString(9, request.getParameter("effort"));
        ps.setString(10, request.getParameter("boxPath"));
        ps.executeUpdate();

        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().print("{\"success\": \"Timesheet entry added successfully.\"}");
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "SQL Error while adding timesheet entry", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().print("{\"error\": \"Failed to add timesheet entry.\"}");
    } finally {
        closeResources(null, ps, conn);
    }
}


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String entryId = pathInfo.substring(1);

            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonString = sb.toString();

            try {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(jsonString);

                String epic = (String) jsonObject.get("epic");
                String feature = (String) jsonObject.get("feature");
                String application = (String) jsonObject.get("application");
                String urDescription = (String) jsonObject.get("ur_description");
                String release = (String) jsonObject.get("release");
                String changeNo = (String) jsonObject.get("change_no");
                String remarks = (String) jsonObject.get("remarks");
                String workscope = (String) jsonObject.get("workscope");
                String effort = (String) jsonObject.get("effort");
                String boxPath = (String) jsonObject.get("boxPath");

                updateTimeSheetEntry(entryId, epic, feature, application, urDescription, release, changeNo, remarks, workscope, effort, boxPath);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("{\"success\": \"Timesheet entry updated successfully.\"}");
            } catch (ParseException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\": \"Invalid JSON format.\"}");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print("{\"error\": \"Failed to update timesheet entry.\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Invalid URL.\"}");
        }
    }

    protected void updateTimeSheetEntry(String entryId, String epic, String feature, String application, 
                                        String urDescription, String release, String changeNo, String remarks,
                                        String workscope, String effort, String boxPath) throws SQLException, Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = new DatabaseUtils().createConnection();
            String query = "UPDATE TIMESHEETDB.timesheet_entries SET epic = ?, feature = ?, application = ?, ur_description = ?, release_date = ?, change_no = ?, remarks = ?, workscope = ?, effort = ?, boxPath = ? WHERE id = ?";
            ps = conn.prepareStatement(query);

            ps.setString(1, epic != null && !epic.isEmpty() ? epic : null);
            ps.setString(2, feature != null && !feature.isEmpty() ? feature : null);
            ps.setString(3, application != null && !application.isEmpty() ? application : null);
            ps.setString(4, urDescription != null && !urDescription.isEmpty() ? urDescription : null);
            ps.setString(5, release != null && !release.isEmpty() ? release : null);
            ps.setString(6, changeNo != null && !changeNo.isEmpty() ? changeNo : null);
            ps.setString(7, remarks != null && !remarks.isEmpty() ? remarks : null);
            ps.setString(8, workscope != null && !workscope.isEmpty() ? workscope : null);
            ps.setString(9, effort != null && !effort.isEmpty() ? effort : null);
            ps.setString(10, boxPath != null && !boxPath.isEmpty() ? boxPath : null);
            ps.setInt(11, Integer.parseInt(entryId));

            int updatedRows = ps.executeUpdate();
            System.out.println("Rows updated: " + updatedRows);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error updating timesheet entry: " + e.getMessage());
        } finally {
            closeResources(null, ps, conn);
        }
    }




    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String entryId = pathInfo.substring(1);
            try {
                deleteTimeSheetEntry(entryId, response);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to delete timesheet entry", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print("{\"error\": \"Failed to delete timesheet entry.\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Invalid URL.\"}");
        }
    }

    private void deleteTimeSheetEntry(String entryId, HttpServletResponse response) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = new DatabaseUtils().createConnection();
            String query = "DELETE FROM TIMESHEETDB.timesheet_entries WHERE id = ?";
            ps = conn.prepareStatement(query);
            ps.setInt(1, Integer.parseInt(entryId));
            ps.executeUpdate();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("{\"success\": \"Timesheet entry deleted successfully.\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error while deleting timesheet entry", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Failed to delete timesheet entry.\"}");
        } finally {
            closeResources(null, ps, conn);
        }
    }

    private void closeResources(ResultSet rs, PreparedStatement ps, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to close resources", ex);
        }
    }
}
