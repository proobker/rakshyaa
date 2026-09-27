import { Router, raw } from "express";
import fs from "fs";
import crypto from "crypto";
import { deleteBlob, getBlob, getUser, listBlobs, upsertBlob } from "../db.js";
import { blobPath, readableBlobPath } from "../storage.js";

export const backupRouter = Router();
backupRouter.get("/", (req, res) => {
  res.json({ blobs: listBlobs(req.user!.sub).map(b => ({
    key: b.key, kind: b.kind, size: b.size, checksum: b.checksum, updatedAt: b.updated_at
  })) });
});
backupRouter.get("/me", (req, res) => {
  const row = getUser(req.user!.sub)!;
  res.json({user: {sub:row.google_sub,email:row.email,name:row.name,picture:row.picture,phone:row.phone,bio:row.bio}});
});
backupRouter.use(raw({type:"application/octet-stream",limit:"50mb"}));
for (const kind of ["data", "media"] as const) {
  backupRouter.put("/"+kind+"/:key", (req, res) => {
    if (!Buffer.isBuffer(req.body)) { res.status(415).json({error:"Use application/octet-stream"}); return; }
    const account = getUser(req.user!.sub);
    if (!account || account.session_version !== req.accountVersion) {
      res.status(401).json({error:"Account no longer exists"}); return;
    }
    const value = req.params.key!;
    if (!/^[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}$/.test(value)) {
      res.status(400).json({error:"Invalid backup key"}); return;
    }
    const key = kind === "media" ? "media:"+value : value;
    fs.writeFileSync(blobPath(req.user!.sub,key),req.body);
    upsertBlob(crypto.randomUUID(),req.user!.sub,key,kind,req.body.length,
      crypto.createHash("sha256").update(req.body).digest("hex"));
    res.json({ok:true,key,id:value,size:req.body.length});
  });
  backupRouter.get("/"+kind+"/:key", (req,res) => {
    const key = kind === "media" ? "media:"+req.params.key! : req.params.key!;
    if (!getBlob(req.user!.sub,key)) { res.status(404).json({error:"Blob not found"}); return; }
    const file = readableBlobPath(req.user!.sub,key);
    if (!fs.existsSync(file)) { res.status(404).json({error:"Blob not found"}); return; }
    res.type("application/octet-stream").send(fs.readFileSync(file));
  });
  backupRouter.delete("/"+kind+"/:key", (req,res) => {
    const key = kind === "media" ? "media:"+req.params.key! : req.params.key!;
    if (getBlob(req.user!.sub,key)) {
      const file = readableBlobPath(req.user!.sub,key);
      if (fs.existsSync(file)) fs.unlinkSync(file);
      deleteBlob(req.user!.sub,key);
    }
    res.json({ok:true});
  });
}
