import { Router } from "express";
import { syncUserFromGoogle, verifyGoogleIdToken, issueSession } from "../auth.js";

export const authRouter = Router();

/**
 * POST /auth/google
 * Body: { "idToken": "<google id token>" }
 * Verifies the Google ID token and exchanges it for a Rakshyaa session JWT.
 */
authRouter.post("/google", async (req, res) => {
  try {
    const { idToken } = req.body ?? {};
    if (!idToken || typeof idToken !== "string") {
      res.status(400).json({ error: "Missing idToken" });
      return;
    }

    const payload = await verifyGoogleIdToken(idToken);
    const user = syncUserFromGoogle(payload);
    const token = issueSession(user);

    res.json({ token, user });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unknown error";
    console.error("Google token verification failed:", message);
    res.status(401).json({ error: "Google token verification failed" });
  }
});
