<?php
/**
 * send_notification.php
 * VPS पर यह file upload करें: /var/www/html/send_notification.php
 * 
 * साथ में service-account.json भी upload करें (same folder में)
 * service-account.json: Firebase Console → Project Settings → Service Accounts → Generate New Private Key
 */

header('Content-Type: application/json');

// ✅ Security: Simple secret key (Android app में भी same key होगी)
define('SECRET_KEY', 'rmplus_notif_secret_2024');

// ✅ Firebase Project ID
define('PROJECT_ID', 'rm-plus');

// ✅ Path to service account JSON (same folder में रखें)
define('SERVICE_ACCOUNT_FILE', __DIR__ . '/service-account.json');

// ─── INPUT ────────────────────────────────────────
$secret  = $_POST['secret']  ?? '';
$token   = $_POST['token']   ?? '';
$topic   = $_POST['topic']   ?? '';
$title   = $_POST['title']   ?? '';
$message = $_POST['message'] ?? '';

// ─── VALIDATION ───────────────────────────────────
if ($secret !== SECRET_KEY) {
    http_response_code(403);
    echo json_encode(['error' => 'Unauthorized']);
    exit;
}

if ((empty($token) && empty($topic)) || empty($title)) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing token/topic or title']);
    exit;
}

if (!file_exists(SERVICE_ACCOUNT_FILE)) {
    http_response_code(500);
    echo json_encode(['error' => 'Service account file not found']);
    exit;
}

// ─── GET OAUTH2 TOKEN FROM GOOGLE ─────────────────
$sa = json_decode(file_get_contents(SERVICE_ACCOUNT_FILE), true);
if (!$sa) {
    http_response_code(500);
    echo json_encode(['error' => 'Invalid service account JSON']);
    exit;
}

$accessToken = getAccessToken($sa);
if (!$accessToken) {
    http_response_code(500);
    echo json_encode(['error' => 'Failed to get access token']);
    exit;
}

// ─── SEND FCM PUSH ────────────────────────────────
$result = sendFcmPush($accessToken, $token, $topic, $title, $message);
echo json_encode($result);


// ══════════════════════════════════════════════════
// FUNCTIONS
// ══════════════════════════════════════════════════

function getAccessToken($sa) {
    $now = time();

    $header  = base64url_encode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $payload = base64url_encode(json_encode([
        'iss'   => $sa['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud'   => 'https://oauth2.googleapis.com/token',
        'iat'   => $now,
        'exp'   => $now + 3600,
    ]));

    $signing_input = "$header.$payload";

    $key = openssl_pkey_get_private($sa['private_key']);
    if (!$key) return null;

    openssl_sign($signing_input, $signature, $key, 'SHA256');
    $jwt = "$signing_input." . base64url_encode($signature);

    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion'  => $jwt,
        ]),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => true,
    ]);

    $response = curl_exec($ch);
    curl_close($ch);

    $data = json_decode($response, true);
    return $data['access_token'] ?? null;
}

function sendFcmPush($accessToken, $token, $topic, $title, $body) {
    $projectId = PROJECT_ID;
    $url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send";

    $messageData = [
        'notification' => [
            'title' => $title,
            'body'  => $body,
        ],
        'data' => [
            'title'   => $title,
            'message' => $body,
        ],
        'android' => [
            'priority' => 'high',
            'notification' => [
                'sound'      => 'default',
                'channel_id' => 'rmplus_channel',
            ],
        ],
    ];

    if (!empty($topic)) {
        $messageData['topic'] = $topic;
    } else {
        $messageData['token'] = $token;
    }

    $payload = json_encode(['message' => $messageData]);

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $payload,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER     => [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json',
        ],
        CURLOPT_SSL_VERIFYPEER => true,
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $result = json_decode($response, true) ?? [];
    $result['http_code'] = $httpCode;
    return $result;
}

function base64url_encode($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}
