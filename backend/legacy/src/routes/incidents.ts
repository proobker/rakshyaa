import { Router } from "express";
import crypto from "crypto";
import { config } from "../config.js";
import { createIncident, getIncident, listActiveIncidents, updateIncidentStatus } from "../db.js";

import { requireAuth } from "../middleware/auth.js";

export const incidentsRouter = Router();

/**
 * POST /incidents
 * Body: { status, latitude?, longitude?, activatedAt? }
 * Authenticated users create an incident (e.g. SOS activation).
 */
incidentsRouter.post("/", requireAuth, (req, res) => {
  const user = req.user!;
  const { status, latitude, longitude, activatedAt } = req.body ?? {};
  const requestedId = req.body?.id;
  if (requestedId !== undefined && (typeof requestedId !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(requestedId))) {
    res.status(400).json({ error: "Invalid incident id" }); return;
  }
  if ((latitude != null && (typeof latitude !== "number" || !Number.isFinite(latitude) || Math.abs(latitude) > 90)) ||
      (longitude != null && (typeof longitude !== "number" || !Number.isFinite(longitude) || Math.abs(longitude) > 180)) ||
      (activatedAt != null && (!Number.isSafeInteger(activatedAt) || activatedAt < 0)) ||
      (status != null && status !== "active")) {
    res.status(400).json({ error: "Invalid incident details" }); return;
  }
  const id = requestedId ?? crypto.randomUUID();
  const existing = getIncident(id);
  if (existing) {
    if (existing.user_id !== user.sub) { res.status(409).json({error:"Incident id unavailable"}); return; }
    res.json({ok:true,id}); return;
  }
  createIncident({
    id,
    user_id: user.sub,
    status: status ?? "active",
    latitude: typeof latitude === "number" ? latitude : (req.body?.latitude ?? null),
    longitude: typeof longitude === "number" ? longitude : (req.body?.longitude ?? null),
    activated_at: activatedAt ?? Date.now(),
  });
  res.status(201).json({ ok: true, id });
});

/**
 * POST /incidents/:id/resolve
 * Marks an incident as resolved.
 */
incidentsRouter.post("/:id/resolve", requireAuth, (req, res) => {
  const user = req.user!;
  const id = req.params.id!;
  if (!updateIncidentStatus(id, user.sub, "resolved")) {
    res.status(404).json({ error: "Incident not found" }); return;
  }
  res.json({ ok: true, id });
});

/**
 * GET /incidents/admin/active
 * Requires x-api-key header matching ADMIN_API_KEY. Used by the admin portal.
 */
incidentsRouter.get("/admin/active", (req, res) => {
  const key = req.headers["x-api-key"];
  if (key !== config.adminApiKey) {
    res.status(403).json({ error: "Forbidden" });
    return;
  }
  res.json({ incidents: listActiveIncidents() });
});
