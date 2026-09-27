import dotenv from "dotenv";
import path from "path";
import fs from "fs";

dotenv.config();

function required(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export const config = {
  supportEmail: process.env.SUPPORT_EMAIL ?? "",
  port: Number(process.env.PORT ?? 8080),
  googleWebClientId: required("GOOGLE_WEB_CLIENT_ID"),
  jwtSecret: required("JWT_SECRET"),
  jwtExpirySeconds: Number(process.env.JWT_EXPIRY_SECONDS ?? 60 * 60 * 24 * 30), // 30 days
  dataDir: required("DATA_DIR", path.resolve(__dirname, "..", "data")),
  dbPath: required("DB_PATH", path.resolve(__dirname, "..", "data", "rakshyaa.db")),
  adminApiKey: required("ADMIN_API_KEY"),
  overpassUrl: required(
    "OVERPASS_URL",
    "https://overpass-api.de/api/interpreter"
  ),
};

// Ensure the data + media directories exist.
fs.mkdirSync(path.join(config.dataDir, "media"), { recursive: true });

if (process.env.NODE_ENV === "production" &&
    (config.jwtSecret.length < 32 || config.adminApiKey.length < 32)) {
  throw new Error("Production JWT_SECRET and ADMIN_API_KEY must each contain at least 32 characters.");
}
