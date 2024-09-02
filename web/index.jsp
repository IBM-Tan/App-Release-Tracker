<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Timesheet Application</title>
    <link rel="stylesheet" type="text/css" href="css/styles.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
</head>
<body>
    <div class="filter-container">
        <label for="filterApplication">Filter by Application:</label>
        <select id="filterApplication">
            <option value="">All</option>
            <option value="FRC">FRC</option>
            <option value="FCC">FCC</option>
            <option value="FOR">FOR</option>
            <option value="IFT">IFT</option>
            <option value="WEBSIG">WEBSIG</option>
            <option value="CSV">CSV</option>
            <option value="ATS">ATS</option>
            <option value="DTC">DTC</option>
            <option value="PS04">PS04</option>
            <option value="PSS">PSS</option>
            <option value="SWEEP">SWEEP</option>
            <option value="Account Register">Account Register</option>
            <option value="NSIP">NSIP</option>
        </select>
        <!-- HTML for filtering -->
        <label for="filterDay">Day:</label>
        <input type="number" id="filterDay" placeholder="Day">
        <label for="filterMonth">Month:</label>
        <input type="number" id="filterMonth" placeholder="Month">
        <label for="filterYear">Year:</label>
        <input type="number" id="filterYear" placeholder="Year">
        <label for="filterDateRangeStart">Start Date:</label>
        <input type="date" id="filterDateRangeStart">
        <label for="filterDateRangeEnd">End Date:</label>
        <input type="date" id="filterDateRangeEnd">

        <input type="text" id="filterRelease" placeholder="Filter by Release">
    </div>
    <div class="table-container">
        <div class="table-wrapper">
        <table id="timesheetTable">
            <thead>
                <tr>
                    <th style="width: 10%;">EPIC</th>
                    <th style="width: 10%;">Feature</th>
                    <th style="width: 10%;">Application</th>
                    <th style="width: 25%;">UR Description</th>
                    <th style="width: 10%;">Release</th>
                    <th style="width: 15%;">Change No.</th>
                    <th style="width: 10%;">Remarks</th>
                    <th style="width: 10%;">Actions</th>
                </tr>
            </thead>
            <tbody>
                <!-- Rows will be added dynamically here -->
            </tbody>
        </table>
        </div>
    </div>
    <div class="buttons">
        <button>+</button>
    </div>

    <div id="passwordDialog" class="dialog">
        <div class="dialog-content">
            <h3>Confirm Changes</h3>
            <p>Please enter the password to confirm changes:</p>
            <input type="password" id="dialogPasswordInput">
            <div class="dialog-actions">
                <button class="dialog-confirm-button">Confirm</button>
                <button class="dialog-cancel-button">Cancel</button>
            </div>
        </div>
    </div>
    
    <div id="deleteDialog" class="dialog">
        <div class="dialog-content">
            <p>Are you sure you want to delete this row?</p>
            <div class="dialog-actions">
                <button class="dialog-confirm-button">Confirm</button>
                <button class="dialog-cancel-button">Cancel</button>
            </div>
        </div>
    </div>

        <!-- Bottom Sheet Structure -->
    <div id="detailsBottomSheet">
        <span id="closeBottomSheet">&times;</span>
        <div class="bottom-sheet-content">
            <p><strong>Scope of Work:</strong> <span id="scopeWorkDisplay"></span></p>
            <p><strong>Estimation Effort:</strong> <span id="estimationEffortDisplay"></span></p>
            <p><strong>Box Path:</strong> <span id="boxPathDisplay"></span></p>
        </div>
    </div>


    <!-- Edit Details Dialog Structure -->
    <div id="detailsDialog" class="modern-dialog">
        <div class="dialog-content">
            <h3>Edit Details</h3>
            <div class="input-group">
                <label for="dialogScopeInput">Scope of Work:</label>
                <textarea id="dialogScopeInput"></textarea>
            </div>
            <div class="input-group">
                <label for="dialogEffortInput">Estimation Effort:</label>
                <input type="text" id="dialogEffortInput" class="small-input">
            </div>
            <div class="input-group">
                <label for="dialogBoxPathInput">Box Path:</label>
                <input type="text" id="dialogBoxPathInput" class="small-input">
            </div>
            <div class="dialog-buttons">
                <button class="dialog-cancel-button">Cancel</button>
                <button class="dialog-save-button">Save</button>
            </div>
        </div>
    </div>





    <script src="js/scripts.js"></script>
</body>
</html>