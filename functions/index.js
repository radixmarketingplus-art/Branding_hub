const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// ✅ Trigger: जब भी notifications/{uid}/{notifId} में नया data आए
exports.sendPushOnNotification = functions.database
    .ref("/notifications/{uid}/{notifId}")
    .onCreate(async (snapshot, context) => {
        const uid = context.params.uid;
        const data = snapshot.val();

        if (!data) return null;

        const title = data.title || "RM Ads Maker";
        const message = data.message || "You have a new notification";

        try {
            // User का FCM token Firebase से लो
            const tokenSnap = await admin.database()
                .ref(`/users/${uid}/fcmToken`)
                .once("value");

            const token = tokenSnap.val();
            if (!token) {
                console.log(`No FCM token for user: ${uid}`);
                return null;
            }

            // FCM v1 API से push भेजो
            const fcmMessage = {
                token: token,
                notification: {
                    title: title,
                    body: message,
                },
                data: {
                    title: title,
                    message: message,
                },
                android: {
                    priority: "high",
                    notification: {
                        sound: "default",
                        channelId: "rmplus_channel",
                    },
                },
            };

            const response = await admin.messaging().send(fcmMessage);
            console.log(`✅ Notification sent to ${uid}: ${response}`);
            return response;

        } catch (error) {
            console.error(`❌ Error sending notification to ${uid}:`, error);
            return null;
        }
    });
