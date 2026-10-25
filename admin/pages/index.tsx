import type { NextPage } from "next";
import Head from "next/head";
import { useRef, useState } from "react";
import { fetchActiveIncidents, type Incident } from "../lib/apiClient";

function relativeTime(timestamp: number, now: Date) {
  const minutes = Math.round((now.getTime() - timestamp) / 60000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  return `${Math.round(hours / 24)} d ago`;
}

const Home: NextPage = () => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [updated, setUpdated] = useState<Date | null>(null);
  const keyInput = useRef<HTMLInputElement>(null);
  const apiKey = useRef("");
  const requestVersion = useRef(0);

  async function refresh(key: string) {
    const version = ++requestVersion.current;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchActiveIncidents(key);
      if (version !== requestVersion.current) return;
      apiKey.current = key;
      if (keyInput.current) keyInput.current.value = "";
      setIncidents(data);
      setConnected(true);
      setUpdated(new Date());
    } catch {
      if (version !== requestVersion.current) return;
      setError("Unable to load incidents. Check your access key and backend connection, then retry.");
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }

  function signOut() {
    requestVersion.current++;
    apiKey.current = "";
    setConnected(false);
    setIncidents([]);
    setUpdated(null);
    setError(null);
    setLoading(false);
  }

  const located = incidents.filter((inc) => inc.latitude != null && inc.longitude != null).length;
  const oldest = incidents.length ? Math.min(...incidents.map((inc) => inc.activated_at)) : null;

  return (
    <div className="shell">
      <Head>
        <title>Rakshyaa | Safety operations</title>
        <meta name="robots" content="noindex,nofollow" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="theme-color" content="#006b5e" />
        <link rel="icon" href="/icon.png" />
      </Head>
      <header className="topbar">
        <a className="brand" href="/">
          <img src="/icon.png" alt="" width={36} height={36} />
          <span>Rakshyaa<small>Safety operations</small></span>
        </a>
        <span className={`badge ${connected ? "live" : ""}`}>
          <i aria-hidden="true" />{connected ? "Connected" : "Operator portal"}
        </span>
      </header>

      <main>
        {!connected ? (
          <section className="hero">
            <div className="hero-copy">
              <p className="eyebrow">Incident response</p>
              <h1>A clear view when it matters.</h1>
              <p className="lede">Review active SOS incidents and their reported locations from one calm, focused desk.</p>
              <ul className="facts">
                <li><strong>Manual refresh</strong><span>Records update only when you refresh.</span></li>
                <li><strong>Session-only key</strong><span>Your access key is held in memory and cleared on disconnect.</span></li>
                <li><strong>Not a dispatch log</strong><span>An incident row is not proof of dispatch or acknowledgement.</span></li>
              </ul>
            </div>
            <div className="card sign-in">
              <h2>Connect to your response desk</h2>
              <p>Use your operator access key. It stays in memory for this session.</p>
              <form onSubmit={(event) => { event.preventDefault(); void refresh(keyInput.current?.value.trim() ?? ""); }}>
                <label htmlFor="access-key">Operator access key</label>
                <input id="access-key" ref={keyInput} type="password" required autoComplete="off" placeholder="••••••••••••" />
                <button disabled={loading}>{loading ? "Connecting…" : "Connect securely"}</button>
              </form>
              {error && <p className="alert" role="alert">{error}</p>}
            </div>
          </section>
        ) : (
          <section className="desk">
            <div className="toolbar">
              <div>
                <p className="eyebrow">Response queue</p>
                <h1 className="desk-title">Active incidents</h1>
              </div>
              <div className="actions">
                <button disabled={loading} onClick={() => void refresh(apiKey.current)}>{loading ? "Refreshing…" : "Refresh incidents"}</button>
                <button className="secondary" onClick={signOut}>Disconnect</button>
              </div>
            </div>

            <div className="stats">
              <div className="stat"><span>Active</span><strong>{incidents.length}</strong></div>
              <div className="stat"><span>With location</span><strong>{located}</strong></div>
              <div className="stat"><span>Oldest activation</span><strong className="small">{oldest != null && updated ? relativeTime(oldest, updated) : "—"}</strong></div>
              <div className="stat"><span>Last update</span><strong className="small">{updated?.toLocaleTimeString() ?? "—"}</strong></div>
            </div>

            {error && (
              <div className="alert" role="alert">
                <strong>Showing the last successful response.</strong> This information may be out of date. {error}
              </div>
            )}

            <div className="card table-card">
              {incidents.length === 0 ? (
                <div className="empty">
                  <div className="empty-icon" aria-hidden="true">✓</div>
                  <h3>No active incidents in this response</h3>
                  <p>New incidents appear when you refresh.</p>
                </div>
              ) : (
                <table>
                  <caption>Active SOS incidents from the latest successful response</caption>
                  <thead><tr><th scope="col">Incident</th><th scope="col">User</th><th scope="col">Status</th><th scope="col">Reported location</th><th scope="col">Activated</th></tr></thead>
                  <tbody>{incidents.map((inc) => (
                    <tr key={inc.id}>
                      <td data-label="Incident" title={inc.id}><code>{inc.id.slice(0, 8)}</code></td>
                      <td data-label="User" title={inc.user_id}><code>{inc.user_id.slice(0, 8)}</code></td>
                      <td data-label="Status"><span className="status">{inc.status}</span></td>
                      <td data-label="Location">{inc.latitude != null && inc.longitude != null ? <a target="_blank" rel="noreferrer" href={`https://maps.google.com/?q=${inc.latitude},${inc.longitude}`}>{inc.latitude.toFixed(4)}, {inc.longitude.toFixed(4)} ↗</a> : <span className="muted">Location unavailable</span>}</td>
                      <td data-label="Activated">{new Date(inc.activated_at).toLocaleString()}</td>
                    </tr>
                  ))}</tbody>
                </table>
              )}
            </div>
            <p className="note">Refresh to check for new incidents. Coordinates are the initial SOS location and are not updated live.</p>
          </section>
        )}
      </main>
      <footer>Rakshyaa · Access restricted to authorized response personnel</footer>

      <style jsx global>{`
        :root {
          --bg: #f4f7f3; --surface: #ffffff; --surface-2: #eef4ef; --line: #dbe5dc;
          --text: #142d25; --muted: #52675d; --brand: #006b5e; --brand-ink: #ffffff;
          --brand-soft: #c2f0e2; --danger: #942b22; --danger-soft: #ffebe7; --focus: #b76817;
          --shadow: 0 1px 2px rgba(20, 45, 37, .06), 0 12px 32px -12px rgba(20, 45, 37, .18);
        }
        @media (prefers-color-scheme: dark) {
          :root {
            --bg: #0e1915; --surface: #15241e; --surface-2: #1c2e26; --line: #2a3f35;
            --text: #e0ebe3; --muted: #a3b9ae; --brand: #85d5be; --brand-ink: #00382d;
            --brand-soft: #005144; --danger: #ffb4ab; --danger-soft: #3d1a17; --focus: #f0a35a;
            --shadow: 0 1px 2px rgba(0, 0, 0, .3), 0 12px 32px -12px rgba(0, 0, 0, .5);
          }
        }
        * { box-sizing: border-box; }
        body { margin: 0; background: var(--bg); color: var(--text); font-family: Inter, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; -webkit-font-smoothing: antialiased; }
        button, input { font: inherit; }
        button, a, input { -webkit-tap-highlight-color: transparent; }
        :focus-visible { outline: 3px solid var(--focus); outline-offset: 3px; }
      `}</style>
      <style jsx>{`
        .shell { min-height: 100vh; display: flex; flex-direction: column; background: radial-gradient(1200px 500px at 85% -10%, color-mix(in srgb, var(--brand-soft) 55%, transparent), transparent 70%); }
        .topbar, main, footer { width: 100%; max-width: 1200px; margin: 0 auto; padding-left: 32px; padding-right: 32px; }
        .topbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding-top: 20px; padding-bottom: 20px; }
        .brand { display: flex; align-items: center; gap: 12px; color: var(--text); text-decoration: none; }
        .brand img { border-radius: 10px; }
        .brand span { font-size: 20px; font-weight: 750; letter-spacing: -.02em; line-height: 1.1; }
        .brand small { display: block; font-size: 12px; font-weight: 500; letter-spacing: .02em; color: var(--muted); }
        .badge { display: inline-flex; align-items: center; gap: 8px; border: 1px solid var(--line); background: var(--surface); border-radius: 100px; padding: 8px 14px; font-size: 12px; font-weight: 650; color: var(--muted); white-space: nowrap; }
        .badge i { width: 8px; height: 8px; border-radius: 50%; background: var(--muted); }
        .badge.live { color: var(--brand); }
        .badge.live i { background: #1fa37a; box-shadow: 0 0 0 4px color-mix(in srgb, #1fa37a 25%, transparent); }
        main { flex: 1; padding-top: 40px; padding-bottom: 40px; }
        .eyebrow { margin: 0; font-size: 12px; font-weight: 750; letter-spacing: .14em; text-transform: uppercase; color: var(--brand); }
        h1 { font-size: clamp(34px, 5vw, 56px); letter-spacing: -.04em; line-height: 1.05; margin: 14px 0; }
        h2 { font-size: 22px; letter-spacing: -.02em; margin: 0 0 6px; }
        p { color: var(--muted); line-height: 1.6; margin: 0; }
        .hero { display: grid; grid-template-columns: 1.1fr .9fr; gap: 48px; align-items: center; }
        .lede { font-size: 18px; max-width: 520px; }
        .facts { list-style: none; padding: 0; margin: 32px 0 0; display: grid; gap: 14px; }
        .facts li { display: grid; gap: 2px; padding-left: 18px; border-left: 3px solid var(--brand-soft); }
        .facts strong { font-size: 15px; }
        .facts span { color: var(--muted); font-size: 14px; }
        .card { background: var(--surface); border: 1px solid var(--line); border-radius: 20px; box-shadow: var(--shadow); }
        .sign-in { padding: 32px; }
        form { display: grid; gap: 10px; margin-top: 24px; }
        label { font-size: 14px; font-weight: 650; }
        input { border: 1px solid var(--line); background: var(--surface-2); color: var(--text); border-radius: 12px; padding: 14px 16px; width: 100%; }
        input:focus { border-color: var(--brand); }
        button { min-height: 48px; border: 0; border-radius: 12px; padding: 12px 20px; background: var(--brand); color: var(--brand-ink); font-weight: 650; cursor: pointer; transition: transform .12s ease, filter .12s ease; white-space: nowrap; }
        button:hover:not(:disabled) { filter: brightness(1.08); }
        button:active:not(:disabled) { transform: translateY(1px); }
        button:disabled { opacity: .6; cursor: wait; }
        form button { margin-top: 8px; }
        .secondary { background: var(--surface); color: var(--text); border: 1px solid var(--line); }
        .alert { margin-top: 16px; padding: 14px 16px; background: var(--danger-soft); color: var(--danger); border-radius: 12px; line-height: 1.5; }
        .toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
        .desk-title { font-size: clamp(30px, 4vw, 42px); margin-bottom: 0; }
        .actions { display: flex; gap: 10px; flex-wrap: wrap; }
        .stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin: 28px 0 0; }
        .stat { background: var(--surface); border: 1px solid var(--line); border-radius: 16px; padding: 18px 20px; display: grid; gap: 6px; min-width: 0; }
        .stat span { font-size: 13px; color: var(--muted); font-weight: 550; }
        .stat strong { font-size: 32px; letter-spacing: -.03em; line-height: 1.1; font-variant-numeric: tabular-nums; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .stat strong.small { font-size: 20px; padding-top: 8px; }
        .table-card { margin-top: 20px; overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }
        caption { text-align: left; padding: 18px 20px 6px; color: var(--muted); font-size: 13px; }
        th { color: var(--muted); font-size: 12px; font-weight: 650; text-transform: uppercase; letter-spacing: .06em; }
        td, th { padding: 16px 20px; border-bottom: 1px solid var(--line); white-space: nowrap; }
        tbody tr:last-child td { border-bottom: 0; }
        tbody tr:hover { background: var(--surface-2); }
        code { font-family: ui-monospace, "Cascadia Code", Menlo, monospace; font-size: 13px; background: var(--surface-2); padding: 3px 8px; border-radius: 6px; }
        td a { color: var(--brand); font-weight: 600; text-decoration: none; }
        td a:hover { text-decoration: underline; }
        .muted { color: var(--muted); }
        .status { display: inline-flex; align-items: center; gap: 6px; background: var(--danger-soft); color: var(--danger); padding: 5px 12px; border-radius: 100px; font-weight: 650; font-size: 13px; text-transform: capitalize; }
        .status::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
        .empty { text-align: center; padding: 64px 16px; }
        .empty-icon { width: 56px; height: 56px; margin: 0 auto 8px; border-radius: 50%; display: grid; place-items: center; background: var(--brand-soft); color: var(--brand); font-size: 24px; font-weight: 700; }
        .empty h3 { margin: 12px 0 6px; }
        .note { font-size: 13px; margin-top: 14px; }
        footer { font-size: 12px; color: var(--muted); padding-top: 24px; padding-bottom: 28px; border-top: 1px solid var(--line); }
        @media (max-width: 900px) {
          .hero { grid-template-columns: 1fr; gap: 32px; }
          .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
        }
        @media (max-width: 640px) {
          .topbar, main, footer { padding-left: 16px; padding-right: 16px; }
          main { padding-top: 20px; }
          .sign-in { padding: 22px; }
          .actions, .actions button { width: 100%; }
          .actions button { flex: 1; }
          .table-card { background: transparent; border: 0; box-shadow: none; overflow: visible; }
          table, tbody, tr, td { display: block; width: 100%; }
          thead { display: none; }
          caption { display: block; padding: 0 0 10px; }
          tr { background: var(--surface); border: 1px solid var(--line); border-radius: 16px; padding: 6px 16px; margin-bottom: 12px; }
          td { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; white-space: normal; text-align: right; }
          td::before { content: attr(data-label); color: var(--muted); font-size: 13px; text-align: left; }
          tbody tr:last-child td { border-bottom: 1px solid var(--line); }
          tr td:last-child { border-bottom: 0 !important; }
        }
      `}</style>
    </div>
  );
};
export default Home;
