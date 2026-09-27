import express from "express";
import cors from "cors";
import helmet from "helmet";
import { config } from "./config.js";
import { authRouter } from "./routes/auth.js";
import { backupRouter } from "./routes/backup.js";
import { incidentsRouter } from "./routes/incidents.js";
import { userRouter } from "./routes/user.js";
import { placesRouter } from "./routes/places.js";
import { requireAuth } from "./middleware/auth.js";

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ extended: true, limit: "50mb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "rakshyaa-backend", ts: Date.now() });
});

app.use("/auth", authRouter);

// Everything below requires a valid session JWT (or is API-key protected).
app.use("/backup", requireAuth, backupRouter);
app.use("/incidents", incidentsRouter);
app.use("/user", requireAuth, userRouter);
app.use("/places", requireAuth, placesRouter);

app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const status = (err as Error & { status?: number }).status;
  res.status(status === 413 ? 413 : 500).json({ error: status === 413 ? "Upload too large" : "Internal server error" });
});

app.listen(config.port, () => {
  console.log(`Rakshyaa backend listening on http://localhost:${config.port}`);
});
