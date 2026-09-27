import type { NextPage } from "next";
import Head from "next/head";
import { useRef, useState } from "react";
import { fetchActiveIncidents, type Incident } from "../lib/apiClient";

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

  return (
    <main>
      <Head>
        <title>Rakshyaa | Safety operations</title>
        <meta name="robots" content="noindex,nofollow" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </Head>
      <header>
        <a className="brand" href="/">Rakshyaa<span>Safety operations</span></a>
        <span className="badge">OPERATOR PORTAL</span>
      </header>
      <section className="intro">
        <p className="eyebrow">INCIDENT RESPONSE</p>
        <h1>A clear view when it matters.</h1>
        <p>Review active SOS incidents and their reported locations.</p>
      </section>
      {!connected ? (
        <section className="panel sign-in">
          <h2>Connect to your response desk</h2>
          <p>Use your operator access key. It stays in memory for this session.</p>
          <form onSubmit={(event) => { event.preventDefault(); void refresh(keyInput.current?.value.trim() ?? ""); }}>
            <label htmlFor="access-key">Operator access key</label>
            <input id="access-key" ref={keyInput} type="password" required autoComplete="off" />
            <button disabled={loading}>{loading ? "Connecting…" : "Connect securely"}</button>
          </form>
        </section>
      ) : (
        <section className="panel">
          <div className="toolbar">
            <div><p className="eyebrow">RESPONSE QUEUE</p><h2>{incidents.length} active incidents</h2></div>
            <div className="actions">
              <button disabled={loading} onClick={() => void refresh(apiKey.current)}>{loading ? "Refreshing…" : "Refresh incidents"}</button>
              <button className="secondary" onClick={signOut}>Disconnect</button>
            </div>
          </div>
          <p className="timestamp">Last successful update: {updated?.toLocaleTimeString()}. Refresh to check for new incidents.</p>
          {error && <p className="stale">Showing the last successful response. This information may be out of date.</p>}
          {incidents.length === 0 ? <div className="empty"><h3>No active incidents in this response</h3><p>New incidents appear when you refresh.</p></div> : (
            <div className="table-scroll">
              <table>
                <caption>Active SOS incidents from the latest successful response</caption>
                <thead><tr><th scope="col">Incident</th><th scope="col">User</th><th scope="col">Status</th><th scope="col">Reported location</th><th scope="col">Activated</th></tr></thead>
                <tbody>{incidents.map((inc) => (
                  <tr key={inc.id}>
                    <td title={inc.id}>{inc.id.slice(0, 8)}</td>
                    <td title={inc.user_id}>{inc.user_id.slice(0, 8)}</td>
                    <td><span className="status">{inc.status}</span></td>
                    <td>{inc.latitude != null && inc.longitude != null ? <a target="_blank" rel="noreferrer" href={`https://maps.google.com/?q=${inc.latitude},${inc.longitude}`}>{inc.latitude.toFixed(4)}, {inc.longitude.toFixed(4)} ↗</a> : "Location unavailable"}</td>
                    <td>{new Date(inc.activated_at).toLocaleString()}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </section>
      )}
      {error && <p className="error" role="alert">{error}</p>}
      <footer>Rakshyaa · Access restricted to authorized response personnel</footer>
      <style jsx global>{`
        * { box-sizing: border-box; }
        body { margin: 0; background: #f4f7f3; color: #193a30; font-family: system-ui, sans-serif; }
        button, input { font: inherit; }
        button, a, input { -webkit-tap-highlight-color: transparent; }
        :focus-visible { outline: 3px solid #b76817; outline-offset: 4px; }
      `}</style>
      <style jsx>{`
        main { max-width: 1200px; padding: 32px; margin: auto; }
        header, .toolbar, .actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
        header { padding-bottom: 28px; border-bottom: 1px solid #cedbd1; }
        .brand { color: #006b5e; font-size: 28px; font-weight: 750; text-decoration: none; letter-spacing: -1px; }
        .brand span { display: block; font-size: 12px; letter-spacing: .04em; font-weight: 500; color: #52675d; }
        .badge, .eyebrow { font-size: 11px; font-weight: 750; letter-spacing: .14em; }
        .badge { border: 1px solid #cedbd1; border-radius: 100px; padding: 10px 14px; }
        .intro { padding: 52px 0 32px; }
        h1 { font-size: clamp(32px, 5vw, 52px); letter-spacing: -.045em; max-width: 700px; margin: 12px 0; line-height: 1.12; }
        p { color: #52675d; line-height: 1.6; }
        .panel { background: #fff; border: 1px solid #dbe5dc; border-radius: 24px; padding: 28px; }
        .sign-in { max-width: 560px; }
        h2 { font-size: 23px; letter-spacing: -.025em; margin: 8px 0 16px; }
        form { display: grid; gap: 12px; margin-top: 28px; }
        label { font-size: 14px; font-weight: 650; }
        input { border: 1px solid #8ca99a; border-radius: 10px; padding: 14px; width: 100%; }
        button { min-height: 48px; border: 0; border-radius: 12px; padding: 12px 20px; background: #006b5e; color: white; font-weight: 650; cursor: pointer; }
        button:disabled { opacity: .6; cursor: wait; }
        .secondary { background: #edf3ee; color: #254f3f; }
        .timestamp { font-size: 13px; }
        .table-scroll { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }
        caption { text-align: left; padding: 12px 0; color: #52675d; }
        th { color: #52675d; font-size: 12px; font-weight: 650; background: #f4f7f3; }
        td, th { padding: 18px 12px; border-bottom: 1px solid #e4ebe5; white-space: nowrap; }
        td a { color: #006b5e; }
        .status { background: #ffebe7; color: #942b22; padding: 6px 10px; border-radius: 100px; font-weight: 650; }
        .empty { text-align: center; padding: 48px 16px; background: #f7faf7; border-radius: 16px; margin-top: 24px; }
        .empty h3 { margin-bottom: 0; }
        .error, .stale { padding: 16px; background: #ffebe7; color: #942b22; border-radius: 12px; }
        footer { font-size: 12px; color: #52675d; padding: 28px 0; }
        @media (max-width: 600px) { main { padding: 20px; } .panel { padding: 20px; } .intro { padding-top: 32px; } .actions { width: 100%; } }
      `}</style>
    </main>
  );
};
export default Home;
