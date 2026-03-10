<?php
/**
 * RMPLUS NOTIFICATION INSTALLER
 * 
 * HOW TO USE:
 * 1. Upload ONLY this file to /var/www/html/rmplus_installer.php
 * 2. Open in browser: http://187.77.184.84/rmplus_installer.php
 * 3. It will create send_notification.php and service-account.json automatically
 * 4. DELETE this installer file after setup!
 */

$INSTALL_KEY = 'rmplus_install_2024'; // Security key

if ($_GET['key'] ?? '' !== $INSTALL_KEY) {
    die('<h2>Access Denied</h2><p>Add ?key=rmplus_install_2024 to URL</p>');
}

$results = [];

// ══════════════════════════════════════════════════
// FILE 1: service-account.json
// ══════════════════════════════════════════════════
$serviceAccount = '{
  "type": "service_account",
  "project_id": "YOUR_PROJECT_ID",
  "private_key_id": "YOUR_PRIVATE_KEY_ID",
  "private_key": "-----BEGIN PRIVATE KEY-----\\nYOUR_PRIVATE_KEY\\n-----END PRIVATE KEY-----\\n",
  "client_email": "YOUR_CLIENT_EMAIL",
  "client_id": "YOUR_CLIENT_ID",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "YOUR_CLIENT_CERT_URL",
  "universe_domain": "googleapis.com"
}';

$r1 = file_put_contents(__DIR__ . '/service-account.json', $serviceAccount);
$results[] = $r1 !== false
    ? '✅ service-account.json created successfully'
    : '❌ Failed to create service-account.json (check folder permissions)';

// ══════════════════════════════════════════════════
// FILE 2: send_notification.php
// ══════════════════════════════════════════════════
$notifScript = '<?php
header(\'Content-Type: application/json\');
define(\'SECRET_KEY\', \'rmplus_notif_secret_2024\');
define(\'PROJECT_ID\', \'rm-plus\');
define(\'SERVICE_ACCOUNT_FILE\', __DIR__ . \'/service-account.json\');

$secret  = $_POST[\'secret\']  ?? \'\';
$token   = $_POST[\'token\']   ?? \'\';
$title   = $_POST[\'title\']   ?? \'\';
$message = $_POST[\'message\'] ?? \'\';

if ($secret !== SECRET_KEY) { http_response_code(403); echo json_encode([\'error\' => \'Unauthorized\']); exit; }
if (empty($token) || empty($title)) { http_response_code(400); echo json_encode([\'error\' => \'Missing token or title\']); exit; }
if (!file_exists(SERVICE_ACCOUNT_FILE)) { http_response_code(500); echo json_encode([\'error\' => \'Service account not found\']); exit; }

$sa = json_decode(file_get_contents(SERVICE_ACCOUNT_FILE), true);
$accessToken = getAccessToken($sa);
if (!$accessToken) { http_response_code(500); echo json_encode([\'error\' => \'Auth failed\']); exit; }

echo json_encode(sendFcmPush($accessToken, $token, $title, $message));

function getAccessToken($sa) {
    $now = time();
    $header  = base64url_encode(json_encode([\'alg\' => \'RS256\', \'typ\' => \'JWT\']));
    $payload = base64url_encode(json_encode([\'iss\' => $sa[\'client_email\'], \'scope\' => \'https://www.googleapis.com/auth/firebase.messaging\', \'aud\' => \'https://oauth2.googleapis.com/token\', \'iat\' => $now, \'exp\' => $now + 3600]));
    $signing_input = "$header.$payload";
    $key = openssl_pkey_get_private($sa[\'private_key\']);
    if (!$key) return null;
    openssl_sign($signing_input, $signature, $key, \'SHA256\');
    $jwt = "$signing_input." . base64url_encode($signature);
    $ch = curl_init(\'https://oauth2.googleapis.com/token\');
    curl_setopt_array($ch, [CURLOPT_POST => true, CURLOPT_POSTFIELDS => http_build_query([\'grant_type\' => \'urn:ietf:params:oauth:grant-type:jwt-bearer\', \'assertion\' => $jwt]), CURLOPT_RETURNTRANSFER => true]);
    $response = curl_exec($ch); curl_close($ch);
    $data = json_decode($response, true);
    return $data[\'access_token\'] ?? null;
}

function sendFcmPush($accessToken, $token, $title, $body) {
    $url = "https://fcm.googleapis.com/v1/projects/" . PROJECT_ID . "/messages:send";
    $payload = json_encode([\'message\' => [\'token\' => $token, \'notification\' => [\'title\' => $title, \'body\' => $body], \'data\' => [\'title\' => $title, \'message\' => $body], \'android\' => [\'priority\' => \'high\', \'notification\' => [\'sound\' => \'default\', \'channel_id\' => \'rmplus_channel\']]]]);
    $ch = curl_init($url);
    curl_setopt_array($ch, [CURLOPT_POST => true, CURLOPT_POSTFIELDS => $payload, CURLOPT_RETURNTRANSFER => true, CURLOPT_HTTPHEADER => [\'Authorization: Bearer \' . $accessToken, \'Content-Type: application/json\']]);
    $response = curl_exec($ch); curl_close($ch);
    return json_decode($response, true) ?? [\'error\' => \'No response\'];
}

function base64url_encode($data) { return rtrim(strtr(base64_encode($data), \'+/\', \'-_\'), \'=\'); }
';

$r2 = file_put_contents(__DIR__ . '/send_notification.php', $notifScript);
$results[] = $r2 !== false
    ? '✅ send_notification.php created successfully'
    : '❌ Failed to create send_notification.php (check folder permissions)';

// ══════════════════════════════════════════════════
// SHOW RESULTS
// ══════════════════════════════════════════════════
?>
<!DOCTYPE html>
<html>
<head><title>RMPlus Notification Installer</title>
<style>body{font-family:sans-serif;max-width:600px;margin:50px auto;padding:20px;background:#f5f5f5} .card{background:white;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1)} h2{color:#333} .result{padding:12px;margin:8px 0;border-radius:8px;font-size:16px} .ok{background:#d4edda;color:#155724} .err{background:#f8d7da;color:#721c24} .warn{background:#fff3cd;color:#856404;margin-top:20px;padding:16px;border-radius:8px}</style>
</head>
<body>
<div class="card">
  <h2>🔔 RMPlus Notification Installer</h2>
  <?php foreach ($results as $r): ?>
    <div class="result <?= strpos($r, '✅') !== false ? 'ok' : 'err' ?>"><?= $r ?></div>
  <?php endforeach; ?>
  
  <div class="warn">
    ⚠️ <strong>Security:</strong> Delete this installer file now!<br>
    Visit: <a href="/send_notification.php">http://187.77.184.84/send_notification.php</a> to verify.
  </div>
</div>
</body>
</html>
