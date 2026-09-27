const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";


export interface Incident {
  id: string;
  user_id: string;
  status: string;
  latitude: number | null;
  longitude: number | null;
  activated_at: number;
  created_at: number;
}

export async function fetchActiveIncidents(apiKey: string): Promise<Incident[]> {
  const endpoint = new URL(BACKEND_URL);
  const localDevelopment = ["localhost", "127.0.0.1", "[::1]"].includes(endpoint.hostname) &&
    typeof window !== "undefined" && ["localhost", "127.0.0.1", "[::1]"].includes(window.location.hostname);
  if (endpoint.protocol !== "https:" && !localDevelopment) {
    throw new Error("Operator access requires an HTTPS backend.");
  }
  const res = await fetch(`${BACKEND_URL}/incidents/admin/active`, {
    headers: { "x-api-key": apiKey },
    cache: "no-store",
    signal: AbortSignal.timeout(15000),
  });
  if (!res.ok) throw new Error(`Failed to fetch incidents: ${res.status}`);
  const data = await res.json();
  if (!Array.isArray(data.incidents)) throw new Error("Invalid incident response");
  return data.incidents;
}
