document.addEventListener("DOMContentLoaded", function () {
    const csvForm = document.querySelector(".csv-import-form");
    const fileInput = document.getElementById("file");
    const dialog = document.getElementById("responseDialog");
    const dialogContent = document.getElementById("dialogContent");
    const dialogClose = document.getElementById("dialogClose");

    csvForm.addEventListener("submit", function (e) {
        e.preventDefault();

        const file = fileInput.files[0];
        if (!file) {
            showDialog("Please select a CSV file to upload.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        fetch("/TimeSheetApp/import-csv", {
            method: "POST",
            body: formData
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
