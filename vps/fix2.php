<?php
// fix2.php - Renames the service account file to the correct name
$files = glob("*_service-account.json");
if ($files) {
    arsort($files);
    $latest = reset($files);
    if (rename($latest, "service-account.json")) {
        echo "Success: Renamed $latest to service-account.json";
    } else {
        echo "Error: Could not rename.";
    }
} else { echo "No files found."; }
?>
