import jwt from "jsonwebtoken";
import { OAuth2Client } from "google-auth-library";
import { config } from "./config.js";
import { getUser, upsertUser } from "./db.js";

const googleClient = new OAuth2Client(config.googleWebClientId);

export interface SessionUser {
  sub: string;
  email: string | null;
  name: string | null;
  picture: string | null;
}

export interface AuthPayload {
  user: SessionUser;
  sessionVersion: string;
  iat: number;
  exp: number;
}

/**
 * Verifies a Google ID token. Returns the verified token payload or throws
 * if the token is invalid, expired, or issued for a different audience.
 */
export async function verifyGoogleIdToken(idToken: string) {
  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: config.googleWebClientId,
  });
  const payload = ticket.getPayload();
  if (!payload || !payload.sub) {
    throw new Error("Invalid Google ID token payload");
  }
  return payload;
}

/** Creates or updates the local user from verified Google profile data. */
export function syncUserFromGoogle(payload: {
  sub: string;
  email?: string | null;
  name?: string | null;
  picture?: string | null;
}): SessionUser {
  const user = {
    sub: payload.sub,
    email: payload.email ?? null,
    name: payload.name ?? null,
    picture: payload.picture ?? null,
  };
  upsertUser({
    google_sub: user.sub,
    email: user.email,
    name: user.name,
    picture: user.picture,
  });
  const existing = getUser(user.sub);
  if (!existing) {
    throw new Error("Failed to persist user");
  }
  return { ...user };
}

/** Signs a session JWT for the given user. */
export function issueSession(user: SessionUser): string {
  const account = getUser(user.sub);
  if (!account) throw new Error("Account not found");
  return jwt.sign({ user, sessionVersion: account.session_version }, config.jwtSecret, {
    expiresIn: config.jwtExpirySeconds,
    algorithm: "HS256",
  });
}

/** Verifies a session JWT and returns the embedded user, or null. */
export function verifySession(token: string): SessionUser | null {
  try {
    const decoded = jwt.verify(token, config.jwtSecret, { algorithms: ["HS256"] }) as AuthPayload;
    if (!decoded.user?.sub || !decoded.sessionVersion) return null;
    const account = getUser(decoded.user.sub);
    if (!account || account.session_version !== decoded.sessionVersion) return null;
    return decoded.user;
  } catch {
    return null;
  }
}

export function getUserFromDb(sub: string) {
  return getUser(sub);
}
