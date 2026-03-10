<?php
// fix.php - Ye script purane timestamp wali file ko rename kar dega
$files = glob("*_send_notification.php");
if ($files) {
    arsort($files); // Get latest file
    $latest = reset($files);
    if (rename($latest, "send_notification.php")) {
        echo "Success: Renamed $latest to send_notification.php";
    } else {
        echo "Error: Could not rename file.";
    }
} else {
    echo "No matching files found. Check if send_notification.php already exists.";
}
?>
