<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="navbar.jsp" %>
<!DOCTYPE html>
<html>
<head>
    <title>CSV Import</title>
    <link rel="stylesheet" type="text/css" href="css/importcsv.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <script src="js/importcsv.js" defer></script>
</head>
<body>

    <div class="content-container">
        <div class="csv-import-container">
            <h1>Import CSV Data</h1>
            <!-- Add enctype to allow file upload -->
            <form action="/TimeSheetApp/import-csv" method="post" enctype="multipart/form-data" class="csv-import-form">
                <div class="input-group">
                    <label for="file">Choose CSV File:</label>
                    <input type="file" id="file" name="file" accept=".csv" required>
                </div>
                <button type="submit" class="submit-button">Import CSV</button>
            </form>
        </div>
    </div>

    <div id="responseDialog" class="dialog">
        <div class="dialog-content">
            <p id="dialogContent"></p>
            <button id="dialogClose" class="dialog-close-button">OK</button>
        </div>
    </div>

</body>
</html>
