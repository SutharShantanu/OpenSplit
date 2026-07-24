const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Sends a push notification to a group's members (except the actor) whenever a new
 * activity entry is created. This is the server side of OpenSplit notifications —
 * client apps register their FCM token on users/{uid}.fcmToken, but only a trusted
 * server can fan a message out to other users' devices.
 */
exports.onActivityCreated = functions.firestore
  .document("groups/{groupId}/activity/{activityId}")
  .onCreate(async (snap, context) => {
    const activity = snap.data() || {};
    const groupId = context.params.groupId;

    const groupSnap = await admin.firestore().doc(`groups/${groupId}`).get();
    const group = groupSnap.data();
    if (!group) return null;

    const recipients = (group.memberIds || []).filter(
      (uid) => uid !== activity.actorUid
    );
    if (recipients.length === 0) return null;

    const tokens = [];
    for (const uid of recipients) {
      const userSnap = await admin.firestore().doc(`users/${uid}`).get();
      const token = userSnap.get("fcmToken");
      if (token) tokens.push(token);
    }
    if (tokens.length === 0) return null;

    return admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title: group.name || "OpenSplit",
        body: activity.message || "New activity in your group",
      },
    });
  });
