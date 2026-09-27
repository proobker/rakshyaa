PRAGMA foreign_keys = ON;

CREATE TABLE users (
  google_sub TEXT PRIMARY KEY,
  session_version TEXT NOT NULL,
  email TEXT,
  name TEXT,
  picture TEXT,
  phone TEXT,
  bio TEXT,
  created_at INTEGER NOT NULL
);

CREATE TABLE blobs (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(google_sub) ON DELETE CASCADE,
  key TEXT NOT NULL,
  kind TEXT NOT NULL CHECK (kind IN ('data', 'media')),
  size INTEGER NOT NULL DEFAULT 0,
  checksum TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  UNIQUE(user_id, key)
);

CREATE INDEX idx_blobs_user ON blobs(user_id);

CREATE TABLE blob_parts (
  blob_id TEXT NOT NULL REFERENCES blobs(id) ON DELETE CASCADE,
  part_number INTEGER NOT NULL,
  public_id TEXT NOT NULL,
  secure_url TEXT NOT NULL,
  size INTEGER NOT NULL,
  PRIMARY KEY(blob_id, part_number)
);

CREATE TABLE incidents (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(google_sub) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK (status IN ('active', 'resolved')),
  latitude REAL,
  longitude REAL,
  activated_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE INDEX idx_incidents_user ON incidents(user_id);
CREATE INDEX idx_incidents_status ON incidents(status);
