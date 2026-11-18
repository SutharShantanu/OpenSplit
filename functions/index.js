// Minimal push fan-out. Deploy with `firebase deploy --only functions` from the repo root.
// Each trigger sends an FCM *data* message so the app decides (per user settings) whether to show it.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
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
    groupName: g.get("isDirect") ? "" : g.get("name") || "",
    expenseId: event.params.expenseId,
  });
});

exports.onSettlementCreated = onDocumentCreated("groups/{groupId}/settlements/{settlementId}", async (event) => {
  const s = event.data.data();
  if (!s) return;
  const from = await nameOf(s.fromUid);
  const g = await db.collection("groups").doc(event.params.groupId).get();
  await send([s.toUid], {
    type: "payment",
    title: `${from} paid you`,
    body: `${s.amount} ${s.currency} recorded as ${s.method || "a payment"}`,
    groupId: event.params.groupId,
    groupName: g.get("isDirect") ? "" : g.get("name") || "",
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
    groupName: g.get("name") || "",
    expenseId: a.relatedExpenseId || "",
  });
});

const TIER_LIMITS = {
  free: 20,
  standard: 20,
  pro: 100,
  ultra: 500,
};

async function getUserAiPlanAndLimit(uid) {
  try {
    const userDoc = await db.collection("users").doc(uid).get();
    const data = userDoc.exists ? userDoc.data() : null;
    const rawPlan = (data?.plan || (data?.isPro ? "pro" : "standard")).toLowerCase();
    const plan = (rawPlan === "free" || !TIER_LIMITS[rawPlan]) ? "standard" : rawPlan;
    const dailyLimit = (typeof data?.dailyLimit === "number" && data.dailyLimit > 0)
      ? data.dailyLimit
      : (TIER_LIMITS[plan] || TIER_LIMITS.standard);
    return { plan, dailyLimit };
  } catch (e) {
    return { plan: "standard", dailyLimit: 20 };
  }
}

/**
 * Checks the user's daily AI quota without consuming a request.
 * Daily limit is dynamic based on user's plan: free (20), pro (100), ultra (500).
 */
exports.getDailyAiUsage = onCall({ cors: true }, async (request) => {
  if (!request.auth || !request.auth.uid) {
    return { authenticated: false, plan: "free", usedToday: 0, dailyLimit: 20, remainingToday: 20 };
  }
  const uid = request.auth.uid;
  const { plan, dailyLimit } = await getUserAiPlanAndLimit(uid);
  const today = new Date().toISOString().split("T")[0];
  const doc = await db.collection("users").doc(uid).collection("ai_usage").doc(today).get();
  const usedToday = doc.exists ? (doc.data().count || 0) : 0;
  return {
    authenticated: true,
    plan,
    usedToday,
    dailyLimit,
    remainingToday: Math.max(0, dailyLimit - usedToday)
  };
});

/**
 * Proxy for Gemini AI with Dynamic Daily Quota based on user plan (standard: 20, pro: 100, ultra: 500).
 * Strictly NO pay-as-you-go: once limit is reached, calls are rejected at $0 token cost.
 */
exports.callGeminiProxy = onCall({
  cors: true,
  maxInstances: 10,
}, async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Please sign in with Google to use AI features.");
  }
  const uid = request.auth.uid;
  const { plan, dailyLimit } = await getUserAiPlanAndLimit(uid);

  const today = new Date().toISOString().split("T")[0];
  const usageDocRef = db.collection("users").doc(uid).collection("ai_usage").doc(today);

  let currentUsage = 0;
  await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(usageDocRef);
    currentUsage = (doc.exists ? (doc.data().count || 0) : 0);
    if (currentUsage >= dailyLimit) {
      throw new HttpsError(
        "resource-exhausted",
        `Daily ${plan.toUpperCase()} AI limit reached (${dailyLimit}/${dailyLimit}). Resets tomorrow at 00:00 UTC with no pay-as-you-go; you can continue manually or add your own key in AI Settings.`
      );
    }
    transaction.set(usageDocRef, {
      count: currentUsage + 1,
      lastUsed: admin.firestore.FieldValue.serverTimestamp(),
      date: today
    }, { merge: true });
  });

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey === "unset") {
    throw new HttpsError("failed-precondition", "Gemini API key is not configured on the server.");
  }

  const { prompt, imageBase64, imageMime, maxTokens } = request.data || {};
  if (!prompt) {
    throw new HttpsError("invalid-argument", "Missing prompt parameter.");
  }

  const parts = [{ text: prompt }];
  if (imageBase64) {
    parts.push({
      inline_data: {
        mime_type: imageMime || "image/jpeg",
        data: imageBase64
      }
    });
  }

  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts }],
      generationConfig: {
        maxOutputTokens: maxTokens || 1024,
        temperature: 0.2
      }
    })
  });

  if (!response.ok) {
    const errText = await response.text();
    console.error("Gemini API error:", response.status, errText);
    throw new HttpsError("internal", `Gemini API error: ${response.statusText}`);
  }

  const result = await response.json();
  const text = result?.candidates?.[0]?.content?.parts?.[0]?.text || "";
  const tokens = result?.usageMetadata?.totalTokenCount || 0;

  return {
    text,
    tokens,
    plan,
    usedToday: currentUsage + 1,
    dailyLimit,
    remainingToday: Math.max(0, dailyLimit - (currentUsage + 1))
  };
});
