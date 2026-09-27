import { createRemoteJWKSet, jwtVerify, SignJWT } from "jose";
import { getUser, upsertGoogleUser } from "./db";

const googleKeys = createRemoteJWKSet(new URL("https://www.googleapis.com/oauth2/v3/certs"));

export interface SessionUser {
  sub: string;
  email: string | null;
  name: string | null;
  picture: string | null;
}

export async function exchangeGoogleToken(
  env: Env,
  idToken: string,
): Promise<{ token: string; user: SessionUser }> {
  const { payload } = await jwtVerify(idToken, googleKeys, {
    audience: env.GOOGLE_WEB_CLIENT_ID,
    issuer: ["https://accounts.google.com", "accounts.google.com"],
    algorithms: ["RS256"],
  });
  if (!payload.sub) throw new Error("Invalid Google ID token payload");
  const user: SessionUser = {
    sub: payload.sub,
    email: typeof payload.email === "string" ? payload.email : null,
    name: typeof payload.name === "string" ? payload.name : null,
    picture: typeof payload.picture === "string" ? payload.picture : null,
  };
  const account = await upsertGoogleUser(env.DB, user);
  const seconds = Number(env.JWT_EXPIRY_SECONDS || "2592000");
  const token = await new SignJWT({ user, sessionVersion: account.session_version })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime(Math.floor(Date.now() / 1000) + seconds)
    .sign(new TextEncoder().encode(env.JWT_SECRET));
  return { token, user };
}

export async function verifySession(env: Env, token: string): Promise<SessionUser | null> {
  try {
    const { payload } = await jwtVerify(token, new TextEncoder().encode(env.JWT_SECRET), {
      algorithms: ["HS256"],
    });
    const user = payload.user as SessionUser | undefined;
    const sessionVersion = payload.sessionVersion;
    if (!user?.sub || typeof sessionVersion !== "string") return null;
    const account = await getUser(env.DB, user.sub);
    return account?.session_version === sessionVersion ? user : null;
  } catch {
    return null;
  }
}

export async function secretsEqual(left: string, right: string): Promise<boolean> {
  const bytes = new TextEncoder();
  const [leftHash, rightHash] = await Promise.all([
    crypto.subtle.digest("SHA-256", bytes.encode(left)),
    crypto.subtle.digest("SHA-256", bytes.encode(right)),
  ]);
  const leftBytes = new Uint8Array(leftHash);
  const rightBytes = new Uint8Array(rightHash);
  let difference = 0;
  for (let index = 0; index < leftBytes.length; index++) difference |= leftBytes[index] ^ rightBytes[index];
  return difference === 0;
}
