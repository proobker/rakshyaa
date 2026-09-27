import type { NextFunction, Request, Response } from "express";
import { getUser } from "../db.js";
import { SessionUser, verifySession } from "../auth.js";

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: SessionUser;
      accountVersion?: string;
    }
  }
}

/** Requires a valid Bearer session JWT; populates req.user. */
export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith("Bearer ")) {
    res.status(401).json({ error: "Missing or malformed Authorization header" });
    return;
  }
  const token = header.slice("Bearer ".length).trim();
  const user = verifySession(token);
  if (!user) {
    res.status(401).json({ error: "Invalid or expired session token" });
    return;
  }
  req.user = user;
  req.accountVersion = getUser(user.sub)?.session_version;
  next();
}
