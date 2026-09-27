import { Router } from "express";
import { config } from "../config.js";

export const placesRouter = Router();

const AMENITY_REGEX = "^(hospital|clinic|doctors|police|fire_station)$";

const SEARCH_RADIUS_M = 50000;

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

export interface PlaceDto {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  type: string;
  distanceMeters: number;
}

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function buildAddress(tags: Record<string, string>): string {
  const street = tags["addr:street"] ?? "";
  const housenumber = tags["addr:housenumber"] ?? "";
  const suburb = tags["addr:suburb"] ?? tags["addr:city"] ?? "";
  const streetPart = [housenumber, street].filter(Boolean).join(" ");
  return [streetPart, suburb].filter(Boolean).join(", ");
}

function toPlace(
  element: OverpassElement,
  lat: number,
  lon: number
): PlaceDto | null {
  const tags = element.tags ?? {};
  const amenity = tags["amenity"] ?? "";
  const type = TYPE_MAP[amenity];
  const placeLat = element.center?.lat ?? element.lat;
  const placeLon = element.center?.lon ?? element.lon;
  if (!type || placeLat === undefined || placeLon === undefined) {
    return null;
  }
  const name = tags["name"] ?? "";
  const displayName =
    name ||
    ["addr:housenumber", "addr:street"].map((k) => tags[k] ?? "").filter(Boolean).join(" ") ||
    type;
  return {
    id: `${element.type}-${element.id}`,
    name: displayName,
    address: buildAddress(tags),
    latitude: placeLat,
    longitude: placeLon,
    type,
    distanceMeters: Math.round(haversineMeters(lat, lon, placeLat, placeLon)),
  };
}

/**
 * GET /places/nearby?lat=&lon=&radiusM=
 * Queries the Overpass API (OpenStreetMap) for hospitals, clinics, doctors,
 * police stations and fire stations around the given coordinates, sorted by
 * distance. The search circle is intentionally wide (50 km) so the app can
 * decide what counts as "nearby" (radiusM) and which is the closest match
 * beyond it — a single provider call covers both cases.
 */
placesRouter.get("/nearby", async (req, res) => {
  const lat = Number(req.query.lat);
  const lon = Number(req.query.lon);
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
    res.status(400).json({ error: "lat and lon are required numeric query params" });
    return;
  }

  const query = `[out:json][timeout:25];
(
  node["amenity"~"${AMENITY_REGEX}"](around:${SEARCH_RADIUS_M},${lat},${lon});
  way["amenity"~"${AMENITY_REGEX}"](around:${SEARCH_RADIUS_M},${lat},${lon});
);
out center 500;`;

  let json: OverpassResponse;
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 25000);
    const response = await fetch(config.overpassUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "rakshyaa-dev/1.1 (women's safety app; +https://rakshyaapp.github.io)",
      },
      body: new URLSearchParams({ data: query }),
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!response.ok) {
      throw new Error(`Overpass responded ${response.status}`);
    }
    json = (await response.json()) as OverpassResponse;
  } catch (error) {
    console.error("Overpass request failed:", error);
    res.status(502).json({ error: "Place lookup failed" });
    return;
  }

  const places: PlaceDto[] = [];
  const seen = new Set<string>();
  for (const element of json.elements ?? []) {
    const place = toPlace(element, lat, lon);
    if (!place) continue;
    // Deduplicate node/way pairs that resolve to the same POI.
    const key = `${place.name}|${place.type}|${place.latitude.toFixed(4)}|${place.longitude.toFixed(4)}`;
    if (seen.has(key)) continue;
    seen.add(key);
    places.push(place);
  }

  places.sort((a, b) => a.distanceMeters - b.distanceMeters);
  res.json({ places: places.slice(0, 25) });
});