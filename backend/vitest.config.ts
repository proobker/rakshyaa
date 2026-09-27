import path from "node:path";
import { cloudflareTest, readD1Migrations } from "@cloudflare/vitest-plugin";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    cloudflareTest(async () => ({
      wrangler: { configPath: "./wrangler.jsonc" },
      miniflare: {
        bindings: {
          TEST_MIGRATIONS: await readD1Migrations(path.join(import.meta.dirname, "migrations")),
          GOOGLE_WEB_CLIENT_ID: "test.apps.googleusercontent.com",
          JWT_SECRET: "test-session-secret-at-least-32-characters",
          ADMIN_API_KEY: "test-admin-secret-at-least-32-characters",
          CLOUDINARY_CLOUD_NAME: "local-cloud",
          CLOUDINARY_API_KEY: "test-key",
          CLOUDINARY_API_SECRET: "test-secret",
        },
      },
    })),
  ],
  test: {
    setupFiles: ["./tests/apply-migrations.ts", "./tests/setup.ts"],
  },
});
