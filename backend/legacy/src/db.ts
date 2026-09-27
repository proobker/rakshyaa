import { DatabaseSync } from "node:sqlite";
import fs from "fs";
import crypto from "crypto";
import path from "path";
import { config } from "./config.js";

fs.mkdirSync(path.dirname(config.dbPath), { recursive: true });

export interface UserRow {
  session_version: string;
  google_sub: string;
  email: string | null;
  name: string | null;
  picture: string | null;
  phone: string | null;
  bio: string | null;
  created_at: number;
}

export interface BlobRow {
  id: string;
  user_id: string;
  key: string;
  kind: "data" | "media";
  size: number;
  checksum: string | null;
  created_at: number;
  updated_at: number;
}

export interface IncidentRow {
  id: string;
  user_id: string;
  status: string;
  latitude: number | null;
  longitude: number | null;
  activated_at: number;
  created_at: number;
}

const db = new DatabaseSync(config.dbPath);

db.exec(`
  PRAGMA journal_mode = WAL;
  PRAGMA foreign_keys = ON;

  CREATE TABLE IF NOT EXISTS users (
    google_sub   TEXT PRIMARY KEY,
    email        TEXT,
    name         TEXT,
    picture      TEXT,
    created_at   INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS blobs (
    id         TEXT PRIMARY KEY,
    user_id    TEXT NOT NULL REFERENCES users(google_sub) ON DELETE CASCADE,
    key        TEXT NOT NULL,
    kind       TEXT NOT NULL,
    size       INTEGER NOT NULL DEFAULT 0,
    checksum   TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    UNIQUE(user_id, key)
  );

  CREATE INDEX IF NOT EXISTS idx_blobs_user ON blobs(user_id);

  CREATE TABLE IF NOT EXISTS incidents (
    id          TEXT PRIMARY KEY,
    user_id     TEXT NOT NULL REFERENCES users(google_sub) ON DELETE CASCADE,
    status      TEXT NOT NULL,
    latitude    REAL,
    longitude   REAL,
    activated_at INTEGER NOT NULL,
    created_at  INTEGER NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_incidents_user ON incidents(user_id);
  CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(status);
`);

function tableHasColumn(table: string, column: string): boolean {
  const cols = db.prepare(`PRAGMA table_info(${table})`).all() as { name: string }[];
  return cols.some((c) => c.name === column);
}

if (!tableHasColumn("users", "phone")) {
  db.exec(`ALTER TABLE users ADD COLUMN phone TEXT`);
}
if (!tableHasColumn("users", "bio")) {
  db.exec(`ALTER TABLE users ADD COLUMN bio TEXT`);
}

if (!tableHasColumn("users", "session_version")) {
  db.exec("ALTER TABLE users ADD COLUMN session_version TEXT");
  db.exec("UPDATE users SET session_version = lower(hex(randomblob(16))) WHERE session_version IS NULL");
}
export function upsertUser(user: {
  google_sub: string;
  email: string | null;
  name: string | null;
  picture: string | null;
}): void {
  db.prepare(
    `INSERT INTO users (google_sub, email, name, picture, created_at, session_version)
     VALUES (?, ?, ?, ?, ?, ?)
     ON CONFLICT(google_sub) DO UPDATE SET
       email = excluded.email,
       name = excluded.name,
       picture = excluded.picture`
  ).run(user.google_sub, user.email, user.name, user.picture, Date.now(), crypto.randomUUID());
}

export function getUser(googleSub: string): UserRow | undefined {
  return db
    .prepare(`SELECT * FROM users WHERE google_sub = ?`)
    .get(googleSub) as UserRow | undefined;
}

/**
 * Updates editable user profile fields, preserving any field not provided.
 * Google (re)sign-in never touches phone/bio so user-entered data survives.
 */
export function updateUserProfile(
  googleSub: string,
  fields: { name?: string | null; phone?: string | null; bio?: string | null; picture?: string | null }
): UserRow | undefined {
  const current = getUser(googleSub);
  if (!current) return undefined;
  const name = fields.name !== undefined ? fields.name : current.name;
  const phone = fields.phone !== undefined ? fields.phone : current.phone;
  const bio = fields.bio !== undefined ? fields.bio : current.bio;
  const picture = fields.picture !== undefined ? fields.picture : current.picture;
  db.prepare(
    `UPDATE users SET name = ?, phone = ?, bio = ?, picture = ? WHERE google_sub = ?`
  ).run(name, phone, bio, picture, googleSub);
  return getUser(googleSub);
}

export function listBlobs(userId: string): BlobRow[] {
  return db
    .prepare(`SELECT * FROM blobs WHERE user_id = ?`)
    .all(userId) as unknown as BlobRow[];
}

export function upsertBlob(
  id: string,
  userId: string,
  key: string,
  kind: "data" | "media",
  size: number,
  checksum: string | null
): void {
  const now = Date.now();
  db.prepare(
    `INSERT INTO blobs (id, user_id, key, kind, size, checksum, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(user_id, key) DO UPDATE SET
       id = excluded.id,
       kind = excluded.kind,
       size = excluded.size,
       checksum = excluded.checksum,
       updated_at = excluded.updated_at`
  ).run(id, userId, key, kind, size, checksum, now, now);
}

export function getBlob(userId: string, key: string): BlobRow | undefined {
  return db
    .prepare(`SELECT * FROM blobs WHERE user_id = ? AND key = ?`)
    .get(userId, key) as BlobRow | undefined;
}

export function getBlobById(id: string): BlobRow | undefined {
  return db.prepare(`SELECT * FROM blobs WHERE id = ?`).get(id) as BlobRow | undefined;
}

export function deleteBlob(userId: string, key: string): boolean {
  const res = db.prepare(`DELETE FROM blobs WHERE user_id = ? AND key = ?`).run(userId, key);
  return Number(res.changes) > 0;
}

export function createIncident(incident: {
  id: string;
  user_id: string;
  status: string;
  latitude: number | null;
  longitude: number | null;
  activated_at: number;
}): void {
  db.prepare(
    `INSERT INTO incidents (id, user_id, status, latitude, longitude, activated_at, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).run(
    incident.id,
    incident.user_id,
    incident.status,
    incident.latitude,
    incident.longitude,
    incident.activated_at,
    Date.now()
  );
}

export function updateIncidentStatus(id: string, userId: string, status: string): boolean {
  const result = db.prepare(`UPDATE incidents SET status = ? WHERE id = ? AND user_id = ?`).run(status, id, userId);
  return Number(result.changes) > 0;
}

export function listActiveIncidents(limit = 50): IncidentRow[] {
  return db
    .prepare(
      `SELECT * FROM incidents WHERE status = 'active' ORDER BY activated_at DESC LIMIT ?`
    )
    .all(limit) as unknown as IncidentRow[];
}

export function deleteUser(userId: string): void {
  db.prepare("DELETE FROM users WHERE google_sub = ?").run(userId);
}

export function getIncident(id: string): IncidentRow | undefined {
  return db.prepare("SELECT * FROM incidents WHERE id = ?").get(id) as IncidentRow | undefined;
}
