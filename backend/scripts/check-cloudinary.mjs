import { readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { createHash, randomUUID } from 'node:crypto';

const env = parseEnv(readFileSync(new URL('../.dev.vars', import.meta.url), 'utf8'));
const base = `https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/raw`;
const headers = { Authorization: `Basic ${Buffer.from(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`).toString('base64')}` };
const publicId = `rakshyaa-deployment-check/${randomUUID()}.enc`;
const payload = 'rakshyaa-private-storage-check';
let uploaded = false;
try {
  const form = new FormData();
  form.set('file', new Blob([payload]), 'check.enc');
  form.set('public_id', publicId);
  form.set('type', 'authenticated');
  const upload = await fetch(`${base}/upload`, { method: 'POST', headers, body: form });
  if (!upload.ok) {
    const detail = await upload.json();
    let message = String(detail.error?.message ?? 'Unknown provider error');
    for (const value of Object.values(env)) if (value) message = message.replaceAll(value, '[redacted]');
    throw new Error(`Cloudinary upload failed: HTTP ${upload.status}: ${message}`);
  }
  uploaded = true;
  const result = await upload.json();
  if (result.type !== 'authenticated') throw new Error('Cloudinary did not store an authenticated asset');
  const anonymousUrl = result.secure_url.replace(/\/s--[^/]+--\//, '/');
  const anonymous = await fetch(anonymousUrl);
  await anonymous.body?.cancel();
  if (anonymous.ok) throw new Error('Anonymous access was unexpectedly permitted');
  const timestamp = Math.floor(Date.now() / 1000);
  const params = new URLSearchParams({ expires_at: String(timestamp + 300), public_id: result.public_id, timestamp: String(timestamp), type: 'authenticated' });
  params.sort();
  params.set('signature', createHash('sha256').update([...params].map(([k, v]) => `${k}=${v}`).join('&') + env.CLOUDINARY_API_SECRET).digest('hex'));
  params.set('api_key', env.CLOUDINARY_API_KEY);
  const download = await fetch(`${base}/download?${params}`);
  if (!download.ok || await download.text() !== payload) throw new Error(`Private download failed: HTTP ${download.status}`);
  console.log('Cloudinary: upload and signed download passed; anonymous download denied.');
} finally {
  if (uploaded) {
    const form = new FormData();
    form.set('public_id', publicId);
    form.set('type', 'authenticated');
    const deleted = await fetch(`${base}/destroy`, { method: 'POST', headers, body: form });
    if (!deleted.ok || (await deleted.json()).result !== 'ok') throw new Error('Temporary test-file cleanup failed');
    console.log('Temporary test file removed.');
  }
}
