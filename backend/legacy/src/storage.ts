import fs from "fs";
import path from "path";
import crypto from "crypto";
import { config } from "./config.js";

export function accountDirectory(userId: string): string {
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(userId)) throw new Error("Invalid account identifier");
  const root = path.resolve(config.dataDir, "media");
  const target = path.resolve(root, userId);
  if (path.dirname(target) !== root) throw new Error("Invalid account directory");
  return target;
}
export function blobPath(userId: string, key: string): string {
  const dir = accountDirectory(userId);
  fs.mkdirSync(dir, { recursive: true });
  return path.join(dir, crypto.createHash("sha256").update(key).digest("hex"));
}
export function readableBlobPath(userId: string, key: string): string {
  const current = blobPath(userId, key);
  if (fs.existsSync(current)) return current;
  if (/^[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}$/.test(key)) {
    const legacy = path.join(accountDirectory(userId), key);
    if (fs.existsSync(legacy)) return legacy;
  }
  return current;
}
export function eraseAccountFiles(userId: string): void {
  const target = accountDirectory(userId);
  fs.rmSync(target, { recursive: true, force: true });
}
