document.addEventListener("DOMContentLoaded", function() {
    let originalScope = "";
    let originalRemarks = "";
    let originalBoxPath = "";
    
     // Save the row order to the database
    function saveRowOrderToDatabase() {
        const rows = document.querySelectorAll("#timesheetTable tbody tr");
        const rowOrder = Array.from(rows).map((row, index) => ({
            id: parseInt(row.getAttribute("data-id")),  // Ensure ID is an integer
            displayOrder: index + 1  // Start order from 1
        }));

        fetch('/TimeSheetApp/updateRowOrder', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(rowOrder)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => { throw new Error(err.error); });
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                console.log("Row order updated successfully");
            } else if (data.error) {
                console.error("Failed to update row order", data.error);
            }
        })
        .catch(error => {
            console.error("Error while updating row order:", error.message);
        });
    }

    // Enable row dragging with drag handle
    function enableRowDragging() {
        const rows = document.querySelectorAll("#timesheetTable tbody tr");
        let draggedRow = null;
        let isSelectingText = false;

        rows.forEach((row) => {
            const dragHandle = row.querySelector('.drag-handle');  // Drag handle for each row

            dragHandle.draggable = true;

            dragHandle.addEventListener('dragstart', function (e) {
                draggedRow = row;
                row.style.opacity = '0.5';
                e.dataTransfer.effectAllowed = 'move';
            });

            dragHandle.addEventListener('dragend', function () {
                row.style.opacity = '1';
                draggedRow = null;
                saveRowOrderToDatabase();  // Save order to database
            });

            row.addEventListener('dragover', function (e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
            });

            row.addEventListener('drop', function (e) {
                e.preventDefault();
                if (draggedRow && draggedRow !== this) {
                    const tbody = document.querySelector("#timesheetTable tbody");
                    const rowsArray = Array.from(tbody.rows);
                    const droppedRowIndex = rowsArray.indexOf(this);
                    const draggedRowIndex = rowsArray.indexOf(draggedRow);

                    if (draggedRowIndex < droppedRowIndex) {
                        tbody.insertBefore(draggedRow, this.nextSibling);
                    } else {
                        tbody.insertBefore(draggedRow, this);
                    }
                    saveRowOrderToDatabase();  // Save order to database
                }
            });
        });
    }

    // Load timesheet entries
    function loadTimesheetEntries() {
        fetch('/TimeSheetApp/timesheet')
            .then(response => response.json())
            .then(data => {
                const tbody = document.getElementById('timesheetTable').getElementsByTagName('tbody')[0];
                tbody.innerHTML = '';  // Clear the table before populating
                data.forEach(entry => {
                    const newRow = createRow(entry);
                    tbody.appendChild(newRow);
                });
                applyColorsToAllRows();
                enableRowDragging();
            })
            .catch(error => console.error('Error loading timesheet entries:', error));
    }


    // create a new row based on an entry
    function createRow(entry) {
        
        const tr = document.createElement('tr');
        tr.setAttribute('data-id', entry.id);  // Set the data-id attribute
        tr.setAttribute('data-scope', entry.workscope || "No scope provided.");
        tr.setAttribute('data-remarks', entry.remarks || "No remarks provided.");
        tr.setAttribute('data-boxpath', entry.boxPath);
        
        // Add a drag handle column (grip icon)
        const dragHandleCell = tr.insertCell();
        dragHandleCell.classList.add('drag-handle');
        dragHandleCell.innerHTML = `<i class="fas fa-grip-vertical"></i>`;


        const columns = ["epic", "feature", "application", "ur_description", "release", "change_no", "effort"];
        columns.forEach((column, index) => {
            const newCell = tr.insertCell();
            const value = entry[column] || '';
            if (column === "application") {
                newCell.innerHTML = `
                    <span class="text">${value}</span>
                    <select class="edit-input application-dropdown " style="display:none;">
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
                        <option value="MoneyGram">MoneyGram</option>
                        <option value="RIA">RIA</option>
                    </select>
                `;
                newCell.querySelector('select').value = value; // Set the correct value for the dropdown
            } else if (column === "release") {
                newCell.innerHTML = `
                    <span class="text">${value}</span>
                    <input type="date" class="edit-input" style="display:none;" value="${value}">
                `;
            } else {
                newCell.innerHTML = `
                    <span class="text">${value.replace(/\n/g, "<br>").replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank">$1</a>')}</span>
                    <textarea class="edit-input" style="display:none;">${value}</textarea>
                `;
            }
        });

        const actionCell = tr.insertCell();
        actionCell.innerHTML = `
            <button class="edit-button" onclick="editRow(this)">
                <i class="fa fa-pencil"></i>
            </button>
            <button class="confirm-button" onclick="showEditDialog(this)" style="display:none;">
                Confirm
            </button>
            <button class="cancel-button" onclick="cancelEdit(this)" style="display:none;">
                Cancel
            </button>
            <button class="edit-details-button" onclick="showDetailsDialog(this)" style="display:none;">
                Edit Details
            </button>
            <button class="delete-button" onclick="showDeleteDialog(this)">
                <i class="fa fa-trash"></i>
            </button>
            <button class="details-button" onclick="showDetailsBottomSheet(this)">
                <i class="fa fa-info-circle"></i>
            </button>
        `;

        applyColorToRow(tr);
        return tr;
    }

    function generateColorFromDate(dateString) {
        // Parse the date string into a Date object
        const date = new Date(dateString);

        // Get the month and year
        const month = date.getMonth(); // 0 (January) to 11 (December)
        const year = date.getFullYear();

        // Predefined hues for each month to ensure distinct colors
        const monthHues = [330, 200, 120, 60, 30, 90, 270, 150, 0, 180, 240, 300]; // Pink, Blue, Green, Yellow, Orange, etc.

        // Get the hue for the current month
        const hue = monthHues[month];

        // Adjust lightness based on the year
        const baseLightness = 65; // Base lightness for the current year
        const yearOffset = year % 10; // Get the last digit of the year
        const lightness = baseLightness + (yearOffset * 3); // Lightness ranges from 65 to 95

        // Set a fixed saturation for consistency
        const saturation = 70; // Saturation at 70%

        return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    }

    function applyColorToRow(row) {
        const releaseCell = row.cells[5];
        const releaseDate = releaseCell.querySelector('.text').textContent.trim();
        if (releaseDate) {
            try {
                const color = generateColorFromDate(releaseDate);
                row.style.backgroundColor = color;
            } catch (error) {
                console.error("Error applying color to row:", error);
                row.style.backgroundColor = ''; // Reset color if there's an error
            }
        } else {
            row.style.backgroundColor = ''; // Reset color if no date
        }
    }

    function applyColorsToAllRows() {
        const table = document.getElementById("timesheetTable").getElementsByTagName('tbody')[0];
        const rows = table.getElementsByTagName('tr');
        for (const row of rows) {
            applyColorToRow(row);
        }
    }

    //add a new row
    function addRow() {
        const table = document.getElementById("timesheetTable").getElementsByTagName('tbody')[0];
        const newRow = table.insertRow();
        const columns = ["EPIC", "Feature", "Application", "UR Description", "Release", "Change No.", "Effort"];
        

        columns.forEach((column, index) => {
            const newCell = newRow.insertCell();
            if (column === "Application") {
                newCell.innerHTML = `
                    <span class="text" contenteditable="false"></span>
                    <select class="edit-input application-dropdown " style="display:none;">
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
                        <option value="MoneyGram">MoneyGram</option>
                        <option value="RIA">RIA</option>
                    </select>
                `;
            } else if (column === "Release") {
                newCell.innerHTML = `
                    <span class="text" contenteditable="false"></span>
                    <input type="date" class="edit-input" style="display:none;">
                `;
            } else {
                newCell.innerHTML = `
                    <span class="text" contenteditable="false"></span>
                    <textarea class="edit-input" style="display:none;"></textarea>
                `;
            }
        });

        const actionCell = newRow.insertCell();
        actionCell.innerHTML = `
            <button class="confirm-button" onclick="confirmAdd(this)">
                Confirm
            </button>
            <button class="cancel-button" onclick="cancelAdd(this)">
                Cancel
            </button>
        `;

        // Focus on the first input field in the new row for easier data entry
        newRow.querySelector('textarea, input, select').focus();
    }

    //confirm adding a new row
    window.confirmAdd = function(button) {
        const tr = button.closest('tr');
        const inputs = tr.querySelectorAll('input, select, textarea');
        const newEntry = {};

        const columns = ["epic", "feature", "application", "ur_description", "release", "change_no", "effort"];

        columns.forEach((column, index) => {
            const inputElement = inputs[index];
            newEntry[column] = inputElement.value.trim();
        });

        fetch('/TimeSheetApp/timesheet', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(newEntry)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                loadTimesheetEntries(); // Reload the table after successful addition
            } else {
                console.error('Failed to add timesheet entry:', data.error);
            }
        })
        .catch(error => console.error('Error:', error));
    };




    // cancel adding a new row
    window.cancelAdd = function(button) {
        const tr = button.closest('tr');
        tr.remove();
    }


    window.deleteRow = function(button) {
        const row = button.closest('tr');
        const id = row.getAttribute('data-id');

        fetch(`/TimeSheetApp/timesheet/${id}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                console.log('Timesheet entry deleted successfully.');
                row.remove();
            } else {
                console.error('Failed to delete timesheet entry:', data.error);
            }
        })
        .catch(error => console.error('Error:', error));
    }


    window.editRow = function(button) {
    const row = button.closest('tr');
    row.querySelectorAll('td').forEach((cell, index) => {
        if (index > 0 && index < 8) { // Skip the first column (drag handle)
            const textarea = cell.querySelector('.edit-input');
            const span = cell.querySelector('.text');
            if (textarea) {
                if (textarea.type === 'date' || textarea.tagName === 'SELECT') {
                    textarea.style.display = 'block';
                    span.style.display = 'none';
                    textarea.value = span.innerHTML;
                } else {
                    textarea.style.display = 'block';
                    span.style.display = 'none';
                    textarea.value = span.innerHTML.replace(/<br\s*\/?>/gi, "\n");
                }
            }
        }
    });

    const actionCell = row.querySelector('td:last-child');
    actionCell.querySelector('.edit-button').style.display = 'none';
    actionCell.querySelector('.confirm-button').style.display = 'inline-block';
    actionCell.querySelector('.cancel-button').style.display = 'inline-block';
    actionCell.querySelector('.details-button').style.display = 'none';
    actionCell.querySelector('.edit-details-button').style.display = 'inline-block';

    originalScope = row.dataset.scope || "No scope provided.";
    originalRemarks = row.dataset.remarks || "No remarks provided.";
    originalBoxPath = row.dataset.boxpath || "No box path provided.";
}

    window.showEditDialog = function(button) {
        const dialog = document.getElementById('passwordDialog');
        dialog.style.display = 'flex';

        const cancelButton = dialog.querySelector('.dialog-cancel-button');
        const confirmButton = dialog.querySelector('.dialog-confirm-button');
        const passwordInput = dialog.querySelector('#dialogPasswordInput');

        cancelButton.onclick = function() {
            dialog.style.display = 'none';
            cancelEdit(button);
        };

        confirmButton.onclick = function() {
            if (passwordInput.value === '1234') {
                applyChanges(button);
                dialog.style.display = 'none';
                passwordInput.value = '';
            } else {
                alert("Incorrect password. Changes were not saved.");
                passwordInput.value = '';
            }
        };
    }

    window.showDeleteDialog = function(button) {
        const dialog = document.getElementById('deleteDialog');
        dialog.style.display = 'flex';

        const cancelButton = dialog.querySelector('.dialog-cancel-button');
        const confirmButton = dialog.querySelector('.dialog-confirm-button');

        cancelButton.onclick = function() {
            dialog.style.display = 'none';
        };

        confirmButton.onclick = function() {
            deleteRow(button);
            dialog.style.display = 'none';
        };
    }

    window.cancelEdit = function(button) {
        const row = button.closest('tr');
        row.querySelectorAll('td').forEach((cell, index) => {
            if (index < 7) {
                const textarea = cell.querySelector('.edit-input');
                const span = cell.querySelector('.text');
                textarea.style.display = 'none';
                span.style.display = 'inline-block';
            }
        });

        const actionCell = row.querySelector('td:last-child');
        actionCell.querySelector('.confirm-button').style.display = 'none';
        actionCell.querySelector('.cancel-button').style.display = 'none';
        actionCell.querySelector('.edit-button').style.display = 'inline-block';
        actionCell.querySelector('.details-button').style.display = 'inline-block';
        actionCell.querySelector('.edit-details-button').style.display = 'none';

        row.dataset.scope = originalScope;
        row.dataset.remarks = originalRemarks;
        row.dataset.boxpath = originalBoxPath;
        
        
    }
    window.applyChanges = function(button) {
    const row = button.closest('tr');
    const id = row.getAttribute('data-id');

    const updatedEntry = {};
    const columns = ["epic", "feature", "application", "ur_description", "release", "change_no", "effort"];
    
    row.querySelectorAll('td').forEach((cell, index) => {
        if (index > 0 && index <= columns.length) { // Skip the drag handle column
            const textarea = cell.querySelector('.edit-input');
            const span = cell.querySelector('.text');
            if (textarea) {
                const columnName = columns[index - 1];  // Adjust index for skipped column
                updatedEntry[columnName] = textarea.value.trim();
                span.innerHTML = textarea.type === 'date' || textarea.tagName === 'SELECT'
                    ? textarea.value
                    : textarea.value.replace(/\n/g, "<br>").replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank">$1</a>');
                textarea.style.display = 'none';
                span.style.display = 'inline-block';
            }
        }
    });
    
    updatedEntry.workscope = row.dataset.scope;
    updatedEntry.remarks = row.dataset.remarks;
    updatedEntry.boxPath = row.dataset.boxpath;

    fetch(`/TimeSheetApp/timesheet/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatedEntry)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('Timesheet entry updated successfully.');
        } else {
            console.error('Failed to update timesheet entry:', data.error);
        }
    })
    .catch(error => console.error('Error:', error));

    const actionCell = row.querySelector('td:last-child');
    actionCell.querySelector('.confirm-button').style.display = 'none';
    actionCell.querySelector('.cancel-button').style.display = 'none';
    actionCell.querySelector('.edit-button').style.display = 'inline-block';
    actionCell.querySelector('.details-button').style.display = 'inline-block';

    const editDetailsButton = actionCell.querySelector('.edit-details-button');
    if (editDetailsButton) {
        editDetailsButton.style.display = 'none';
    }

    applyColorToRow(row);
    saveRowOrderToDatabase();
}



   window.showDetailsDialog = function(button) {
    const dialog = document.getElementById('detailsDialog');
    dialog.style.display = 'flex';

    const cancelButton = dialog.querySelector('.dialog-cancel-button');
    const saveButton = dialog.querySelector('.dialog-save-button');

    const scopeInput = dialog.querySelector('#dialogScopeInput');
    const remarksInput = dialog.querySelector('#dialogRemarksInput');
    const boxPathInput = dialog.querySelector('#dialogBoxPathInput');

    const row = button.closest('tr');

    scopeInput.value = row.dataset.scope || "No scope provided.";
    remarksInput.value = row.dataset.remarks || "No remarks provided.";
    boxPathInput.value = row.dataset.boxpath || "No box path provided.";

    cancelButton.onclick = function() {
        dialog.style.display = 'none';
    };

    saveButton.onclick = function() {
        row.dataset.scope = scopeInput.value;
        row.dataset.remarks = remarksInput.value;
        row.dataset.boxpath = boxPathInput.value;

        dialog.style.display = 'none';
    };
}

   window.showDetailsBottomSheet = function(button) {
        const row = button.closest('tr');

        const scopeOfWork = row.dataset.scope || "No scope provided.";
        const remarks = row.dataset.remarks || "No remarks provided.";
        const boxPath = row.dataset.boxpath || "No box path provided.";

        const bottomSheet = document.getElementById('detailsBottomSheet');

        bottomSheet.querySelector('.bottom-sheet-content').innerHTML = `
            <p><strong>Scope of Work:</strong> ${scopeOfWork}</p>
            <p><strong>Remarks:</strong> ${remarks}</p>
            <p><strong>Box Path:</strong> ${boxPath}</p>
        `;

        bottomSheet.style.display = 'flex';
    }

    document.getElementById('closeBottomSheet').onclick = function() {
        document.getElementById('detailsBottomSheet').style.display = 'none';
    }

    function sortTableByRelease() {
        const table = document.getElementById("timesheetTable");
        const tbody = table.getElementsByTagName('tbody')[0];
        const rows = Array.from(tbody.getElementsByTagName('tr'));

        rows.sort((rowA, rowB) => {
            const dateA = new Date(rowA.cells[4].querySelector('.text').textContent);
            const dateB = new Date(rowB.cells[4].querySelector('.text').textContent);
            return dateA - dateB;
        });

        rows.forEach(row => tbody.appendChild(row));
        applyColorsToAllRows();
    }

    function filterTable() {
    const applicationFilter = document.getElementById("filterApplication").value.toLowerCase().trim();
    const releaseFilterRaw = document.getElementById("filterRelease").value.toLowerCase().trim();
    const dayFilter = document.getElementById("filterDay").value.trim();
    const monthFilter = document.getElementById("filterMonth").value.trim();
    const yearFilter = document.getElementById("filterYear").value.trim();
    const dateRangeStart = document.getElementById("filterDateRangeStart").value;
    const dateRangeEnd = document.getElementById("filterDateRangeEnd").value;

    const table = document.getElementById("timesheetTable").getElementsByTagName('tbody')[0];
    const rows = table.getElementsByTagName('tr');

    // Convert the releaseFilterRaw with * into a regular expression
    const releaseFilterRegex = new RegExp('^' + releaseFilterRaw.replace(/\*/g, '.*') + '$');

    for (const row of rows) {
        let application = row.cells[2].querySelector('.text').textContent.toLowerCase().trim();
        let release = row.cells[4].querySelector('.text').textContent.toLowerCase().trim();

        if (row.cells[2].querySelector('.application-dropdown').style.display !== 'none') {
            application = row.cells[2].querySelector('.application-dropdown').value.toLowerCase().trim();
        }
        if (row.cells[4].querySelector('input[type="date"]').style.display !== 'none') {
            release = row.cells[4].querySelector('input[type="date"]').value.toLowerCase().trim();
        }

        // Convert release date into components: day, month, year
        const releaseDateObj = new Date(release);
        const releaseDay = releaseDateObj.getDate();
        const releaseMonth = releaseDateObj.getMonth() + 1;  // Months are zero-indexed in JS
        const releaseYear = releaseDateObj.getFullYear();

        // Filtering logic for application
        const matchesApplication = applicationFilter === '' || application.includes(applicationFilter);

        // Filtering logic for release with wildcards support
        let matchesRelease = releaseFilterRaw === '' || releaseFilterRegex.test(release);

        // Additional logic for filtering by day, month, year
        if (dayFilter) {
            matchesRelease = matchesRelease && releaseDay === parseInt(dayFilter, 10);
        }
        if (monthFilter) {
            matchesRelease = matchesRelease && releaseMonth === parseInt(monthFilter, 10);
        }
        if (yearFilter) {
            matchesRelease = matchesRelease && releaseYear === parseInt(yearFilter, 10);
        }

        // Filtering by date range
        if (dateRangeStart && dateRangeEnd) {
            const releaseTime = releaseDateObj.getTime();
            const startTime = new Date(dateRangeStart).getTime();
            const endTime = new Date(dateRangeEnd).getTime();
            matchesRelease = matchesRelease && releaseTime >= startTime && releaseTime <= endTime;
        }

        // Show or hide rows based on filtering criteria
        row.style.display = (matchesApplication && matchesRelease) ? '' : 'none';
    }
}

    document.querySelector('.buttons button').addEventListener('click', addRow);
    document.getElementById("filterApplication").addEventListener("change", filterTable);
    document.getElementById("filterRelease").addEventListener("input", filterTable);
    document.getElementById("filterDay").addEventListener("input", filterTable);
    document.getElementById("filterMonth").addEventListener("input", filterTable);
    document.getElementById("filterYear").addEventListener("input", filterTable);
    document.getElementById("filterDateRangeStart").addEventListener("change", filterTable);
    document.getElementById("filterDateRangeEnd").addEventListener("change", filterTable);

    loadTimesheetEntries();
    enableRowDragging();
    applyColorsToAllRows();
});