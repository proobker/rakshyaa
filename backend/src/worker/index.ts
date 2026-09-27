import { Hono, type MiddlewareHandler } from "hono";
import { cors } from "hono/cors";
import { secureHeaders } from "hono/secure-headers";
import { exchangeGoogleToken, secretsEqual, type SessionUser, verifySession } from "./auth";
import { destroyAccountObjects, destroyObject, downloadObject, MAX_UPLOAD_BYTES, uploadEncryptedBlob } from "./cloudinary";
import {
  deleteBlob,
  getBlobWithParts,
  getUser,
  saveBlob,
  updateProfile,
  type BlobRow,
  type IncidentRow,
  type UserRow,
} from "./db";
import { nearbyPlaces } from "./places";

type AppBindings = { Bindings: Env; Variables: { user: SessionUser } };
const app = new Hono<AppBindings>();
const BACKUP_KEY = /^[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}$/;
const INCIDENT_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const requireAuth: MiddlewareHandler<AppBindings> = async (context, next) => {
  const header = context.req.header("authorization");
  if (!header?.startsWith("Bearer ")) {
    return context.json({ error: "Missing or malformed Authorization header" }, 401);
  }
  const user = await verifySession(context.env, header.slice(7).trim());
  if (!user) return context.json({ error: "Invalid or expired session token" }, 401);
  context.set("user", user);
  await next();
};

app.use("*", secureHeaders());
app.use("*", cors());

app.onError((error, context) => {
  console.error(JSON.stringify({ message: "request failed", error: error.message, path: context.req.path }));
  return context.json({ error: "Internal server error" }, 500);
});

app.get("/", (context) =>
  context.json({ ok: true, service: "rakshyaa-backend", message: "Rakshyaa API is running", health: "/health" }),
);

app.get("/health", (context) =>
  context.json({ ok: true, service: "rakshyaa-backend", runtime: "cloudflare-workers", ts: Date.now() }),
);

app.post("/auth/google", async (context) => {
  const body: { idToken?: unknown } = await context.req.json<{ idToken?: unknown }>().catch(() => ({}));
  if (typeof body.idToken !== "string" || !body.idToken) return context.json({ error: "Missing idToken" }, 400);
  try {
    return context.json(await exchangeGoogleToken(context.env, body.idToken));
  } catch (error) {
    console.error(
      JSON.stringify({
        message: "Google token verification failed",
        error: error instanceof Error ? error.message : String(error),
      }),
    );
    return context.json({ error: "Google token verification failed" }, 401);
  }
});

app.get("/incidents/admin/active", async (context) => {
  const key = context.req.header("x-api-key") ?? "";
  if (!context.env.ADMIN_API_KEY || !key || !(await secretsEqual(key, context.env.ADMIN_API_KEY))) return context.json({ error: "Forbidden" }, 403);
  const incidents = await context.env.DB.prepare(
    "SELECT * FROM incidents WHERE status = 'active' ORDER BY activated_at DESC LIMIT 50",
  ).all<IncidentRow>();
  return context.json({ incidents: incidents.results });
});

app.use("/backup", requireAuth);
app.use("/backup/*", requireAuth);
app.use("/user/*", requireAuth);
app.use("/places/*", requireAuth);
app.use("/incidents", requireAuth);
app.use("/incidents/*", requireAuth);

app.get("/user/profile", async (context) => {
  const row = await getUser(context.env.DB, context.get("user").sub);
  return context.json({ user: row ? userDto(row) : context.get("user") });
});

app.put("/user/profile", async (context) => {
  const body: Record<string, unknown> = await context.req.json<Record<string, unknown>>().catch(() => ({}));
  const updated = await updateProfile(context.env.DB, context.get("user").sub, {
    name: typeof body.name === "string" ? body.name : undefined,
    phone: typeof body.phone === "string" ? body.phone : undefined,
    bio: typeof body.bio === "string" ? body.bio : undefined,
    picture: typeof body.picture === "string" ? body.picture : undefined,
  });
  return updated ? context.json({ user: userDto(updated) }) : context.json({ error: "User not found" }, 404);
});

app.delete("/user/account", async (context) => {
  if (context.req.header("x-confirm-delete") !== "delete-my-account") {
    return context.json({ error: "Account deletion confirmation required" }, 400);
  }
  const userId = context.get("user").sub;
  await destroyAccountObjects(context.env, userId);
  await context.env.DB.prepare("DELETE FROM users WHERE google_sub = ?").bind(userId).run();
  return context.json({ ok: true });
});

app.get("/backup", async (context) => {
  const blobs = await context.env.DB.prepare(
    "SELECT key, kind, size, checksum, updated_at FROM blobs WHERE user_id = ? ORDER BY updated_at DESC",
  )
    .bind(context.get("user").sub)
    .all<Pick<BlobRow, "key" | "kind" | "size" | "checksum" | "updated_at">>();
  return context.json({
    blobs: blobs.results.map((blob) => ({
      key: blob.key,
      kind: blob.kind,
      size: blob.size,
      checksum: blob.checksum,
      updatedAt: blob.updated_at,
    })),
  });
});

app.get("/backup/me", async (context) => {
  const row = await getUser(context.env.DB, context.get("user").sub);
  return row ? context.json({ user: userDto(row) }) : context.json({ error: "User not found" }, 404);
});

for (const kind of ["data", "media"] as const) {
  app.put(`/backup/${kind}/:key`, async (context) => {
    const contentType = context.req.header("content-type")?.split(";", 1)[0].trim().toLowerCase();
    if (contentType !== "application/octet-stream") return context.json({ error: "Use application/octet-stream" }, 415);
    const rawKey = context.req.param("key");
    if (!BACKUP_KEY.test(rawKey)) return context.json({ error: "Invalid backup key" }, 400);
    const declaredSize = Number(context.req.header("content-length") ?? "0");
    if (declaredSize > MAX_UPLOAD_BYTES) return context.json({ error: "Upload too large" }, 413);
    const chunks: Uint8Array[] = [];
    const reader = context.req.raw.body?.getReader();
    let size = 0;
    if (reader) {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        size += value.byteLength;
        if (size > MAX_UPLOAD_BYTES) {
          await reader.cancel();
          return context.json({ error: "Upload too large" }, 413);
        }
        chunks.push(value);
      }
    }
    const payload = new ArrayBuffer(size);
    const payloadBytes = new Uint8Array(payload);
    let offset = 0;
    for (const chunk of chunks) { payloadBytes.set(chunk, offset); offset += chunk.byteLength; }
    chunks.length = 0;
    const userId = context.get("user").sub;
    const key = kind === "media" ? `media:${rawKey}` : rawKey;
    const current = await getBlobWithParts(context.env.DB, userId, key);
    const uploaded = await uploadEncryptedBlob(context.env, userId, key, payload);
    try {
      await saveBlob(context.env.DB, current?.blob ?? null, {
      userId,
      key,
      kind,
      size: payload.byteLength,
      checksum: uploaded.checksum,
      parts: uploaded.parts,
      });
    } catch (error) {
      await Promise.allSettled(uploaded.parts.map((part) => destroyObject(context.env, part.publicId)));
      throw error;
    }
    for (const stale of current?.parts ?? []) {
      context.executionCtx.waitUntil(
        destroyObject(context.env, stale.public_id).catch((error) =>
          console.error(JSON.stringify({ message: "stale Cloudinary part cleanup failed", error: String(error) })),
        ),
      );
    }
    return context.json({ ok: true, key, id: rawKey, size: payload.byteLength });
  });

  app.get(`/backup/${kind}/:key`, async (context) => {
    const rawKey = context.req.param("key");
    const key = kind === "media" ? `media:${rawKey}` : rawKey;
    const stored = await getBlobWithParts(context.env.DB, context.get("user").sub, key);
    if (!stored || stored.parts.length === 0) return context.json({ error: "Blob not found" }, 404);
    const first = await downloadObject(context.env, stored.parts[0].public_id);
    if (!first.ok || !first.body) {
      return context.json({ error: "Blob storage unavailable" }, 502);
    }
    let partIndex = 0;
    let reader = first.body.getReader();
    const readable = new ReadableStream<Uint8Array>({
      async pull(controller) {
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (!done) { controller.enqueue(value); return; }
            reader.releaseLock();
            if (++partIndex === stored.parts.length) { controller.close(); return; }
            const response = await downloadObject(context.env, stored.parts[partIndex].public_id);
            if (!response.ok || !response.body) throw new Error("Blob storage unavailable");
            reader = response.body.getReader();
          }
        } catch (error) { controller.error(error); }
      },
      cancel(reason) { return reader.cancel(reason); },
    });
    return new Response(readable, {
      headers: { "Content-Type": "application/octet-stream", "Content-Length": String(stored.blob.size), "Cache-Control": "private, no-store" },
    });
  });

  app.delete(`/backup/${kind}/:key`, async (context) => {
    const rawKey = context.req.param("key");
    const key = kind === "media" ? `media:${rawKey}` : rawKey;
    const stored = await getBlobWithParts(context.env.DB, context.get("user").sub, key);
    if (stored) {
      await Promise.all(stored.parts.map((part) => destroyObject(context.env, part.public_id)));
      await deleteBlob(context.env.DB, context.get("user").sub, key);
    }
    return context.json({ ok: true });
  });
}

app.post("/incidents", async (context) => {
  const body: Record<string, unknown> = await context.req.json<Record<string, unknown>>().catch(() => ({}));
  const id = body.id === undefined ? crypto.randomUUID() : body.id;
  if (typeof id !== "string" || !INCIDENT_ID.test(id)) return context.json({ error: "Invalid incident id" }, 400);
  const latitude = body.latitude;
  const longitude = body.longitude;
  const activatedAt = body.activatedAt;
  if (
    (latitude != null && (typeof latitude !== "number" || !Number.isFinite(latitude) || Math.abs(latitude) > 90)) ||
    (longitude != null && (typeof longitude !== "number" || !Number.isFinite(longitude) || Math.abs(longitude) > 180)) ||
    (activatedAt != null && (typeof activatedAt !== "number" || !Number.isSafeInteger(activatedAt) || activatedAt < 0)) ||
    (body.status != null && body.status !== "active")
  ) {
    return context.json({ error: "Invalid incident details" }, 400);
  }
  const userId = context.get("user").sub;
  const existing = await context.env.DB.prepare("SELECT user_id FROM incidents WHERE id = ?").bind(id).first<{ user_id: string }>();
  if (existing) {
    return existing.user_id === userId
      ? context.json({ ok: true, id })
      : context.json({ error: "Incident id unavailable" }, 409);
  }
  await context.env.DB.prepare(
    `INSERT INTO incidents (id, user_id, status, latitude, longitude, activated_at, created_at)
     VALUES (?, ?, 'active', ?, ?, ?, ?)`,
  )
    .bind(id, userId, latitude ?? null, longitude ?? null, activatedAt ?? Date.now(), Date.now())
    .run();
  return context.json({ ok: true, id }, 201);
});

app.post("/incidents/:id/resolve", async (context) => {
  const result = await context.env.DB.prepare(
    "UPDATE incidents SET status = 'resolved' WHERE id = ? AND user_id = ?",
  )
    .bind(context.req.param("id"), context.get("user").sub)
    .run();
  return (result.meta.changes ?? 0) > 0
    ? context.json({ ok: true, id: context.req.param("id") })
    : context.json({ error: "Incident not found" }, 404);
});

app.get("/places/nearby", async (context) => {
  const lat = Number(context.req.query("lat"));
  const lon = Number(context.req.query("lon"));
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
    return context.json({ error: "lat and lon are required numeric query params" }, 400);
  }
  try {
    return context.json({ places: await nearbyPlaces(context.env, lat, lon) });
  } catch (error) {
    console.error(JSON.stringify({ message: "Overpass request failed", error: String(error) }));
    return context.json({ error: "Place lookup failed" }, 502);
  }
});

function userDto(row: UserRow) {
  return {
    sub: row.google_sub,
    email: row.email,
    name: row.name,
    picture: row.picture,
    phone: row.phone,
    bio: row.bio,
  };
}

export default app;
