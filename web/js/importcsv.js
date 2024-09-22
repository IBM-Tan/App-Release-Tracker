document.addEventListener("DOMContentLoaded", function () {
    const csvForm = document.querySelector(".csv-import-form");
    const filePathInput = document.getElementById("filePath");
    const dialog = document.getElementById("responseDialog");
    const dialogContent = document.getElementById("dialogContent");
    const dialogClose = document.getElementById("dialogClose");

    csvForm.addEventListener("submit", function (e) {
        e.preventDefault();

        const filePath = filePathInput.value;
        if (!filePath) {
            showDialog("Please enter a valid CSV file path.");
            return;
        }

        fetch("/TimeSheetApp/import-csv", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: `filePath=${encodeURIComponent(filePath)}`
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showDialog(data.success);
            } else if (data.error) {
                showDialog(data.error);
            }
        })
        .catch(error => {
            showDialog("An error occurred: " + error.message);
        });
    });

    function showDialog(message) {
        dialogContent.textContent = message;
        dialog.style.display = "flex";
    }

    dialogClose.addEventListener("click", function () {
        dialog.style.display = "none";
    });
});
