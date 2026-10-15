// Minimal push fan-out. Deploy with `firebase deploy --only functions` from the repo root.
// Each trigger sends an FCM *data* message so the app decides (per user settings) whether to show it.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

async function tokensFor(uids) {
  const docs = await Promise.all(uids.map((u) => db.collection("users").doc(u).get()));
  return docs.map((d) => d.get("fcmToken")).filter(Boolean);
}

async function send(uids, data) {
  const tokens = await tokensFor(uids);
  if (!tokens.length) return;
  await admin.messaging().sendEachForMulticast({ tokens, data, android: { priority: "high" } });
}

async function groupMembers(groupId, exceptUid) {
  const g = await db.collection("groups").doc(groupId).get();
  return (g.get("memberIds") || []).filter((u) => u !== exceptUid);
}

async function nameOf(uid) {
  const u = await db.collection("users").doc(uid).get();
  return u.get("displayName") || "Someone";
}

exports.onExpenseCreated = onDocumentCreated("groups/{groupId}/expenses/{expenseId}", async (event) => {
  const e = event.data.data();
  if (!e || e.isDeleted) return;
  const g = await db.collection("groups").doc(event.params.groupId).get();
  const actor = await nameOf(e.createdBy);
  await send(await groupMembers(event.params.groupId, e.createdBy), {
    type: "expense_added",
    title: g.get("isDirect") ? `${actor} added an expense with you` : `${actor} added "${e.description}"`,
    body: `${e.amount} ${e.currency} in ${g.get("isDirect") ? "a direct expense" : g.get("name")}`,
    groupId: event.params.groupId,
    expenseId: event.params.expenseId,
  });
});

exports.onSettlementCreated = onDocumentCreated("groups/{groupId}/settlements/{settlementId}", async (event) => {
  const s = event.data.data();
  if (!s) return;
  const from = await nameOf(s.fromUid);
  await send([s.toUid], {
    type: "payment",
    title: `${from} paid you`,
    body: `${s.amount} ${s.currency} recorded as ${s.method || "a payment"}`,
    groupId: event.params.groupId,
  });
});

exports.onFriendInviteCreated = onDocumentCreated("invites/{inviteId}", async (event) => {
  const inv = event.data.data();
  if (!inv || !inv.email) return;
  const users = await db.collection("users").where("email", "==", inv.email).limit(1).get();
  if (users.empty) return;
  await send([users.docs[0].id], {
    type: "friend_request",
    title: `${inv.inviterName || "Someone"} wants to split expenses with you`,
    body: "Open People to accept or ignore.",
  });
});

exports.onActivityCreated = onDocumentCreated("groups/{groupId}/activity/{activityId}", async (event) => {
  const a = event.data.data();
  if (!a) return;
  const memberEvents = { MEMBER_ADDED: "member_joined", MEMBER_REMOVED: "member_joined" };
  const type = memberEvents[a.type] || (a.type === "EXPENSE_EDITED" || a.type === "EXPENSE_DELETED" ? "expense_edited" : null);
  if (!type) return; // additions and payments have their own triggers above
  const g = await db.collection("groups").doc(event.params.groupId).get();
  const actor = await nameOf(a.actorUid);
  await send(await groupMembers(event.params.groupId, a.actorUid), {
    type,
    title: g.get("name") || "OpenSplit",
    body: `${actor} ${a.message}`,
    groupId: event.params.groupId,
    expenseId: a.relatedExpenseId || "",
  });
});
