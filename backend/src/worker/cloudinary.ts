const CHUNK_BYTES = 8 * 1024 * 1024;
export const MAX_UPLOAD_BYTES = 50 * 1024 * 1024;

interface UploadResult {
  public_id: string;
  secure_url: string;
  bytes: number;
}

function authHeader(env: Env): string {
  return `Basic ${btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`)}`;
}

async function sha256Hex(value: string | ArrayBuffer): Promise<string> {
  const source = typeof value === "string" ? new TextEncoder().encode(value) : value;
  const digest = await crypto.subtle.digest("SHA-256", source);
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

export async function uploadEncryptedBlob(
  env: Env,
  userId: string,
  key: string,
  payload: ArrayBuffer,
): Promise<{ checksum: string; prefix: string; parts: Array<{ publicId: string; secureUrl: string; size: number }> }> {
  const [userHash, keyHash, checksum] = await Promise.all([
    sha256Hex(userId),
    sha256Hex(key),
    sha256Hex(payload),
  ]);
  const prefix = `rakshyaa/${userHash}/${keyHash}/${crypto.randomUUID()}`;
  const bytes = new Uint8Array(payload);
  const parts: Array<{ publicId: string; secureUrl: string; size: number }> = [];

  try {
    for (let offset = 0, partNumber = 0; offset < bytes.byteLength || partNumber === 0; offset += CHUNK_BYTES, partNumber++) {
      const chunk = bytes.slice(offset, Math.min(offset + CHUNK_BYTES, bytes.byteLength));
      const publicId = `${prefix}/${partNumber}.enc`;
      const form = new FormData();
      form.set("file", new Blob([chunk], { type: "application/octet-stream" }), `part-${partNumber}.enc`);
      form.set("public_id", publicId);
      form.set("overwrite", "false");
      form.set("type", "authenticated");
      const response = await fetch(
        `https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/raw/upload`,
        { method: "POST", headers: { Authorization: authHeader(env) }, body: form },
      );
      if (!response.ok) throw new Error(`Cloudinary upload failed (${response.status})`);
      const uploaded = await response.json<UploadResult>();
      parts.push({ publicId: uploaded.public_id, secureUrl: uploaded.secure_url, size: uploaded.bytes });
    }
  } catch (error) {
    await Promise.allSettled(parts.map((part) => destroyObject(env, part.publicId)));
    throw error;
  }
  return { checksum, prefix, parts };
}

export async function destroyObject(env: Env, publicId: string): Promise<void> {
  const form = new FormData();
  form.set("public_id", publicId);
  form.set("invalidate", "true");
  form.set("type", "authenticated");
  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/raw/destroy`,
    { method: "POST", headers: { Authorization: authHeader(env) }, body: form },
  );
  if (!response.ok) throw new Error(`Cloudinary deletion failed (${response.status})`);
}

export async function destroyAccountObjects(env: Env, userId: string): Promise<void> {
  const userHash = await sha256Hex(userId);
  const url = new URL(
    `https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/resources/raw/authenticated`,
  );
  url.searchParams.set("prefix", `rakshyaa/${userHash}/`);
  url.searchParams.set("invalidate", "true");
  let cursor: string | undefined;
  do {
    if (cursor) url.searchParams.set("next_cursor", cursor);
    const response = await fetch(url, { method: "DELETE", headers: { Authorization: authHeader(env) } });
    if (!response.ok) throw new Error(`Cloudinary account cleanup failed (${response.status})`);
    cursor = (await response.json<{ next_cursor?: string }>()).next_cursor;
  } while (cursor);
}

export async function downloadObject(env: Env, publicId: string): Promise<Response> {
  const timestamp = Math.floor(Date.now() / 1000);
  const params = new URLSearchParams({
    expires_at: String(timestamp + 300), public_id: publicId,
    timestamp: String(timestamp), type: "authenticated",
  });
  params.sort();
  const signingInput = [...params].map(([key, value]) => `${key}=${value}`).join("&");
  params.set("signature", await sha256Hex(signingInput + env.CLOUDINARY_API_SECRET));
  params.set("api_key", env.CLOUDINARY_API_KEY);
  return fetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/raw/download?${params}`);
}
