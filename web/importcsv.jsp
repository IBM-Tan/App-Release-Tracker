
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>CSV Import</title>
</head>
<body>
    <h1>Import CSV Data</h1>
    <form action="/TimeSheetApp/import-csv" method="post">
        <label for="filePath">Enter CSV File Path:</label>
        <input type="text" id="filePath" name="filePath" required>
        <button type="submit">Import CSV</button>
    </form>
</body>
</html>

