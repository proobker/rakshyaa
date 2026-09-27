import { Router } from "express";
import { deleteUser, getUser, updateUserProfile, UserRow } from "../db.js";

import { eraseAccountFiles } from "../storage.js";

export const userRouter = Router();

function rowToDto(row: UserRow) {
  return {
    sub: row.google_sub,
    email: row.email,
    name: row.name,
    picture: row.picture,
    phone: row.phone,
    bio: row.bio,
  };
}

/**
 * GET /user/profile
 * Returns the authenticated user's profile read fresh from the database
 * (includes user-editable name, phone, bio and picture).
 */
userRouter.get("/profile", (req, res) => {
  const sessionUser = req.user!;
  const row = getUser(sessionUser.sub);
  if (!row) {
    res.json({ user: sessionUser });
    return;
  }
  res.json({ user: rowToDto(row) });
});

/**
 * PUT /user/profile
 * Body: { name?, phone?, bio?, picture? }
 * Updates only the provided fields and returns the updated profile.
 */
userRouter.put("/profile", (req, res) => {
  const sessionUser = req.user!;
  const body = req.body ?? {};
  const updated = updateUserProfile(sessionUser.sub, {
    name: typeof body.name === "string" ? body.name : undefined,
    phone: typeof body.phone === "string" ? body.phone : undefined,
    bio: typeof body.bio === "string" ? body.bio : undefined,
    picture: typeof body.picture === "string" ? body.picture : undefined,
  });
  if (!updated) {
    res.status(404).json({ error: "User not found" });
    return;
  }
  res.json({ user: rowToDto(updated) });
});
userRouter.delete("/account", (req, res) => {
  if (req.headers["x-confirm-delete"] !== "delete-my-account") {
    res.status(400).json({ error: "Account deletion confirmation required" }); return;
  }
  eraseAccountFiles(req.user!.sub);
  deleteUser(req.user!.sub);
  res.json({ok:true});
});
