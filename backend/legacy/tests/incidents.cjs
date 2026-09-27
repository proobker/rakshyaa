const { test } = require("node:test");
const assert = require("node:assert/strict");
const { mkdtempSync, rmSync } = require("node:fs");
const { tmpdir } = require("node:os");
const path = require("node:path");
const { spawn } = require("node:child_process");
const { once } = require("node:events");
const net = require("node:net");
const { DatabaseSync } = require("node:sqlite");
const jwt = require("jsonwebtoken");
const crypto = require("node:crypto");

test("incident authorization, creation and resolution", async () => {
  const dir = mkdtempSync(path.join(tmpdir(), "rakshyaa-api-test-"));
  const socket = net.createServer();
  socket.listen(0, "127.0.0.1"); await once(socket, "listening");
  const port = socket.address().port;
  await new Promise(resolve => socket.close(resolve));
  const secret = crypto.randomBytes(32).toString("hex");
  const key = crypto.randomBytes(32).toString("hex");
  const child = spawn(process.execPath, ["dist/index.js"], {
    env: { ...process.env, PORT: String(port), DATA_DIR: dir, DB_PATH: path.join(dir, "test.db"),
      GOOGLE_WEB_CLIENT_ID: "test.apps.googleusercontent.com", JWT_SECRET: secret, ADMIN_API_KEY: key },
    stdio: ["ignore", "pipe", "pipe"]
  });
  const base = `http://127.0.0.1:${port}`;
  try {
    let ready = false;
    for (let i=0; i<100; i++) {
      try { if ((await fetch(base + "/health")).ok) { ready=true; break; } } catch {}
      await new Promise(resolve => setTimeout(resolve, 50));
    }
    assert.ok(ready, "test backend starts");
    const db = new DatabaseSync(path.join(dir, "test.db"));
    for (const id of ["owner", "other"]) db.prepare("INSERT INTO users (google_sub, created_at, session_version) VALUES (?, ?, ?)").run(id, Date.now(), "test-version-"+id);
    db.close();
    const token = sub => jwt.sign({sessionVersion:"test-version-"+sub,user: {sub, email:null, name:null, picture:null}}, secret, {expiresIn:"1h"});
    const post = (route, user, body) => fetch(base+route, {method:"POST", headers:{"Content-Type":"application/json",Authorization:`Bearer ${token(user)}`}, body:JSON.stringify(body)});
    assert.equal((await fetch(base+"/incidents/admin/active")).status, 403);
    assert.equal((await fetch(base+"/incidents/admin/active", {headers:{"x-api-key":"wrong"}})).status,403);
    assert.equal((await fetch(base+"/incidents/admin/active?apiKey="+key)).status,403);
    const admin = () => fetch(base+"/incidents/admin/active", {headers:{"x-api-key":key}});
    assert.equal((await admin()).status,200, "operator key works without user JWT");
    assert.equal((await fetch(base+"/incidents", {method:"POST",headers:{"Content-Type":"application/json"},body:"{}"})).status,401);
    const id = crypto.randomUUID();
    const created = await post("/incidents","owner",{id,latitude:27.7,longitude:85.3});
    assert.equal(created.status,201);
    assert.equal((await created.json()).id,id, "preserve client incident id for resolution");
    assert.equal((await post("/incidents","owner",{id,latitude:27.7,longitude:85.3})).status,200,"incident replay is idempotent");
    assert.equal((await admin()).status,200);
    assert.equal((await (await admin()).json()).incidents.length,1);
    assert.equal((await post("/incidents/"+id+"/resolve","other",{})).status,404,"other users cannot resolve");
    assert.equal((await post("/incidents/"+id+"/resolve","owner",{})).status,200);
    assert.equal((await (await admin()).json()).incidents.length,0);
    assert.equal((await post("/incidents","owner",{latitude:100})).status,400);
    const ownerHeaders = {Authorization:`Bearer ${token("owner")}`};
    const otherHeaders = {Authorization:`Bearer ${token("other")}`};
    const put = (user,key,value) => fetch(base+"/backup/media/"+key,{method:"PUT",headers:{
      Authorization:`Bearer ${token(user)}`,"Content-Type":"application/octet-stream"},body:value});
    assert.equal((await put("owner","test-media","encrypted-owner")).status,200);
    assert.equal((await put("other","test-media","encrypted-other")).status,200);
    assert.equal(await (await fetch(base+"/backup/media/test-media",{headers:ownerHeaders})).text(),"encrypted-owner");
    assert.equal((await fetch(base+"/user/account",{method:"DELETE",headers:ownerHeaders})).status,400);
    assert.equal((await fetch(base+"/user/account",{method:"DELETE",headers:{...ownerHeaders,"x-confirm-delete":"delete-my-account"}})).status,200);
    assert.equal((await fetch(base+"/user/profile",{headers:ownerHeaders})).status,401,"deleted account token is revoked");
    assert.equal(require("node:fs").existsSync(path.join(dir,"media","owner")),false);
    assert.equal(await (await fetch(base+"/backup/media/test-media",{headers:otherHeaders})).text(),"encrypted-other","other account preserved");
    const verification = new DatabaseSync(path.join(dir,"test.db"));
    assert.equal(verification.prepare("SELECT count(*) n FROM incidents WHERE user_id = ?").get("owner").n,0);
    assert.equal(verification.prepare("SELECT count(*) n FROM blobs WHERE user_id = ?").get("owner").n,0);
    verification.prepare("INSERT INTO users (google_sub,created_at,session_version) VALUES (?, ?, ?)").run("owner",Date.now(),"new-version");
    verification.close();
    assert.equal((await fetch(base+"/user/profile",{headers:ownerHeaders})).status,401,"old token remains invalid after re-registration");

  } finally {
    const ended = once(child,"exit"); child.kill(); await ended;
    assert.equal(path.dirname(path.resolve(dir)), path.resolve(tmpdir()));
    assert.ok(path.basename(dir).startsWith("rakshyaa-api-test-"));
    rmSync(dir,{recursive:true,force:true});
  }
});
