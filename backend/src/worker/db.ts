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

export interface BlobPartRow {
  blob_id: string;
  part_number: number;
  public_id: string;
  secure_url: string;
  size: number;
}

export interface IncidentRow {
  id: string;
  user_id: string;
  status: "active" | "resolved";
  latitude: number | null;
  longitude: number | null;
  activated_at: number;
  created_at: number;
}

export function getUser(db: D1Database, sub: string): Promise<UserRow | null> {
  return db.prepare("SELECT * FROM users WHERE google_sub = ?").bind(sub).first<UserRow>();
}

export async function upsertGoogleUser(
  db: D1Database,
  user: { sub: string; email: string | null; name: string | null; picture: string | null },
): Promise<UserRow> {
  await db
    .prepare(
      `INSERT INTO users (google_sub, session_version, email, name, picture, created_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(google_sub) DO UPDATE SET
         email = excluded.email,
         name = excluded.name,
         picture = excluded.picture`,
    )
    .bind(user.sub, crypto.randomUUID(), user.email, user.name, user.picture, Date.now())
    .run();
  const stored = await getUser(db, user.sub);
  if (!stored) throw new Error("Failed to persist user");
  return stored;
}

export async function updateProfile(
  db: D1Database,
  sub: string,
  fields: { name?: string; phone?: string; bio?: string; picture?: string },
): Promise<UserRow | null> {
  const current = await getUser(db, sub);
  if (!current) return null;
  await db
    .prepare("UPDATE users SET name = ?, phone = ?, bio = ?, picture = ? WHERE google_sub = ?")
    .bind(
      fields.name ?? current.name,
      fields.phone ?? current.phone,
      fields.bio ?? current.bio,
      fields.picture ?? current.picture,
      sub,
    )
    .run();
  return getUser(db, sub);
}

export async function getBlobWithParts(
  db: D1Database,
  userId: string,
  key: string,
): Promise<{ blob: BlobRow; parts: BlobPartRow[] } | null> {
  const blob = await db
    .prepare("SELECT * FROM blobs WHERE user_id = ? AND key = ?")
    .bind(userId, key)
    .first<BlobRow>();
  if (!blob) return null;
  const parts = await db
    .prepare("SELECT * FROM blob_parts WHERE blob_id = ? ORDER BY part_number")
    .bind(blob.id)
    .all<BlobPartRow>();
  return { blob, parts: parts.results };
}

export async function saveBlob(
  db: D1Database,
  existing: BlobRow | null,
  input: {
    userId: string;
    key: string;
    kind: "data" | "media";
    size: number;
    checksum: string;
    parts: Array<{ publicId: string; secureUrl: string; size: number }>;
  },
): Promise<string> {
  const id = existing?.id ?? crypto.randomUUID();
  const now = Date.now();
  const statements = [
    db
      .prepare(
        `INSERT INTO blobs (id, user_id, key, kind, size, checksum, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(user_id, key) DO UPDATE SET
           kind = excluded.kind,
           size = excluded.size,
           checksum = excluded.checksum,
           updated_at = excluded.updated_at`,
      )
      .bind(id, input.userId, input.key, input.kind, input.size, input.checksum, existing?.created_at ?? now, now),
    db.prepare("DELETE FROM blob_parts WHERE blob_id = ?").bind(id),
    ...input.parts.map((part, index) =>
      db
        .prepare(
          "INSERT INTO blob_parts (blob_id, part_number, public_id, secure_url, size) VALUES (?, ?, ?, ?, ?)",
        )
        .bind(id, index, part.publicId, part.secureUrl, part.size),
    ),
  ];
  await db.batch(statements);
  return id;
}

export async function deleteBlob(db: D1Database, userId: string, key: string): Promise<boolean> {
  const result = await db.prepare("DELETE FROM blobs WHERE user_id = ? AND key = ?").bind(userId, key).run();
  return (result.meta.changes ?? 0) > 0;
}
