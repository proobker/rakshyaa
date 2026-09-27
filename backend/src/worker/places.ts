const AMENITY_REGEX = "^(hospital|clinic|doctors|police|fire_station)$";
const SEARCH_RADIUS_M = 50_000;
const TYPE_MAP: Record<string, string> = {
  hospital: "hospital",
  clinic: "clinic",
  doctors: "clinic",
  police: "police",
  fire_station: "fire",
};

interface OverpassElement {
  type: "node" | "way" | "relation";
  id: number;
  lat?: number;
  lon?: number;
  center?: { lat: number; lon: number };
  tags?: Record<string, string>;
}

interface OverpassResponse {
  elements?: OverpassElement[];
}

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const radius = 6_371_000;
  const toRad = (degrees: number) => (degrees * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export async function nearbyPlaces(env: Env, lat: number, lon: number): Promise<unknown[]> {
  const query = `[out:json][timeout:25];
(
  node["amenity"~"${AMENITY_REGEX}"](around:${SEARCH_RADIUS_M},${lat},${lon});
  way["amenity"~"${AMENITY_REGEX}"](around:${SEARCH_RADIUS_M},${lat},${lon});
);
out center 500;`;
  const response = await fetch(env.OVERPASS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "User-Agent": "rakshyaa/2.0 (women's safety app; +https://rakshya.rabidahal.com.np)",
    },
    body: new URLSearchParams({ data: query }),
    signal: AbortSignal.timeout(25_000),
  });
  if (!response.ok) throw new Error(`Overpass responded ${response.status}`);
  const json = await response.json<OverpassResponse>();
  const seen = new Set<string>();
  const places = [];
  for (const element of json.elements ?? []) {
    const tags = element.tags ?? {};
    const type = TYPE_MAP[tags.amenity ?? ""];
    const latitude = element.center?.lat ?? element.lat;
    const longitude = element.center?.lon ?? element.lon;
    if (!type || latitude === undefined || longitude === undefined) continue;
    const name =
      tags.name ||
      [tags["addr:housenumber"], tags["addr:street"]].filter(Boolean).join(" ") ||
      type;
    const address = [
      [tags["addr:housenumber"], tags["addr:street"]].filter(Boolean).join(" "),
      tags["addr:suburb"] ?? tags["addr:city"] ?? "",
    ]
      .filter(Boolean)
      .join(", ");
    const dedupe = `${name}|${type}|${latitude.toFixed(4)}|${longitude.toFixed(4)}`;
    if (seen.has(dedupe)) continue;
    seen.add(dedupe);
    places.push({
      id: `${element.type}-${element.id}`,
      name,
      address,
      latitude,
      longitude,
      type,
      distanceMeters: Math.round(haversineMeters(lat, lon, latitude, longitude)),
    });
  }
  return places.sort((a, b) => a.distanceMeters - b.distanceMeters).slice(0, 25);
}
