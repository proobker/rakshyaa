import { env, exports } from "cloudflare:workers";
import { SignJWT } from "jose";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { network } from "./network";

async function sessionFor(sub: string): Promise<string> {
  const version = `version-${sub}`;
  await env.DB.prepare(
    "INSERT INTO users (google_sub, session_version, created_at) VALUES (?, ?, ?)",
  )
    .bind(sub, version, Date.now())
    .run();
  return new SignJWT({
    user: { sub, email: null, name: null, picture: null },
    sessionVersion: version,
  })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(new TextEncoder().encode(env.JWT_SECRET));
}

function request(path: string, init?: RequestInit): Promise<Response> {
  return exports.default.fetch(new Request(`https://example.test${path}`, init));
}

describe("Rakshyaa Worker API", () => {
  beforeEach(async () => {
    await env.DB.exec("DELETE FROM users");
  });

  it("reports health without authentication", async () => {
    const response = await request("/health");
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({ ok: true, runtime: "cloudflare-workers" });
  });

  it("requires a session for incident creation", async () => {
    const response = await request("/incidents", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}",
    });
    expect(response.status).toBe(401);
  });

  it("creates, replays and resolves an incident for its owner", async () => {
    const ownerToken = await sessionFor("owner");
    const otherToken = await sessionFor("other");
    const id = crypto.randomUUID();
    const post = (path: string, token: string, body: unknown) =>
      request(path, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
      });

    expect((await post("/incidents", ownerToken, { id, latitude: 27.7, longitude: 85.3 })).status).toBe(201);
    expect((await post("/incidents", ownerToken, { id })).status).toBe(200);
    expect((await post(`/incidents/${id}/resolve`, otherToken, {})).status).toBe(404);
    expect((await post(`/incidents/${id}/resolve`, ownerToken, {})).status).toBe(200);

    const row = await env.DB.prepare("SELECT status FROM incidents WHERE id = ?").bind(id).first<{ status: string }>();
    expect(row?.status).toBe("resolved");
  });

  it("rejects an invalid incident location", async () => {
    const token = await sessionFor("owner");
    const response = await request("/incidents", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ latitude: 100 }),
    });
    expect(response.status).toBe(400);
  });

  it("stores encrypted backup metadata and streams the ciphertext back", async () => {
    const encrypted = "encrypted-owner";
    network.use(
      http.post("https://api.cloudinary.com/v1_1/local-cloud/raw/upload", async ({ request }) => {
        expect((await request.formData()).get("type")).toBe("authenticated");
        return HttpResponse.json({
          public_id: "rakshyaa/user/key/version/0",
          secure_url: "https://storage.example.test/chunk-0.bin",
          bytes: encrypted.length,
        });
      }),
      http.get("https://api.cloudinary.com/v1_1/local-cloud/raw/download", () =>
        new HttpResponse(encrypted, { headers: { "Content-Type": "application/octet-stream" } }),
      ),
    );

    const token = await sessionFor("owner");
    const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/octet-stream" };
    const upload = await request("/backup/media/evidence", { method: "PUT", headers, body: encrypted });
    expect(upload.status).toBe(200);

    const otherToken = await sessionFor("other");
    expect((await request("/backup/media/evidence", { headers: { Authorization: `Bearer ${otherToken}` } })).status).toBe(404);
    expect((await request("/backup/media/evidence")).status).toBe(401);

    const download = await request("/backup/media/evidence", { headers: { Authorization: `Bearer ${token}` } });
    expect(download.status).toBe(200);
    expect(new TextDecoder().decode(await download.arrayBuffer())).toBe(encrypted);

    const metadata = await env.DB.prepare(
      "SELECT b.size, count(p.part_number) AS parts FROM blobs b JOIN blob_parts p ON p.blob_id = b.id WHERE b.user_id = ? GROUP BY b.id",
    )
      .bind("owner")
      .first<{ size: number; parts: number }>();
    expect(metadata).toEqual({ size: encrypted.length, parts: 1 });
  });
});
