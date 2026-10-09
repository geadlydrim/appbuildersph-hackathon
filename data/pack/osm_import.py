"""Build data/pack/osm-makati.source.json from OpenStreetMap (ODbL) via Overpass.

OSM only. Fares are unknown (null) and minutes are estimates (distance / 120 m per min).
Standard library only. One Overpass request at a time.

    python osm_import.py [--out PATH] [--cache-dir DIR] [--refresh]
"""

import argparse
import bisect
import json
import math
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from datetime import datetime, timezone

from shapes import decode_polyline, encode_polyline

EARTH_RADIUS_M = 6_371_000
BOX = (14.525, 120.995, 14.585, 121.065)  # south, west, north, east
USER_AGENT = "commutenity-hackathon/0.1 (geadlydrim)"
OVERPASS_URLS = (
    "https://overpass-api.de/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
)
MAKATI_PSGC_PREFIX = "1380300"  # PSGC code of Makati City
MAKATI_RELATION_ID = 103716  # boundary=administrative, admin_level=6, name=Makati
GAP_MAX_M = 60
STOP_OFF_LINE_MAX_M = 60
SEGMENT_MIN_M = 50
SEGMENT_MAX_M = 6000
M_PER_MIN = 120
PLACE_CAP = 400

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OUT = os.path.join(HERE, "osm-makati.source.json")
DEFAULT_CACHE = os.path.join(HERE, "cache", "overpass")

ROUTES_QUERY = (
    "[out:json][timeout:240];\n"
    'rel["type"="route"]["route"~"^(bus|share_taxi|minibus)$"]({bbox})->.r;\n'
    ".r out geom;\n"
    "node(r.r);\n"
    "out;\n"
)

BARANGAYS_QUERY = (
    "[out:json][timeout:240];\n"
    'rel["name"]["boundary"="administrative"]["admin_level"="10"]({bbox});\n'
    "out geom;\n"
)

BOUNDARY_QUERY = (
    "[out:json][timeout:120];\n"
    "rel({relation_id});\n"
    "way(r);\n"
    "out geom;\n"
)

AMENITIES = (
    "hospital|university|college|school|marketplace|bus_station|townhall|"
    "library|place_of_worship|police|fire_station"
)
PLACES_QUERY = (
    "[out:json][timeout:240];\n"
    "(\n"
    '  nwr["name"]["amenity"~"^({amenities})$"]({bbox});\n'
    '  nwr["name"]["shop"~"^(mall|department_store)$"]({bbox});\n'
    '  nwr["name"]["leisure"="park"]({bbox});\n'
    '  nwr["name"]["railway"="station"]({bbox});\n'
    '  nwr["name"]["public_transport"="station"]({bbox});\n'
    '  nwr["name"]["tourism"~"^(museum|attraction)$"]({bbox});\n'
    '  nwr["name"]["place"~"^(neighbourhood|quarter|suburb|village)$"]({bbox});\n'
    ");\n"
    "out center tags;\n"
)


class OsmImportError(Exception):
    pass


# ---------------------------------------------------------------- geometry


def haversine_m(a, b):
    lat1, lng1 = math.radians(a[0]), math.radians(a[1])
    lat2, lng2 = math.radians(b[0]), math.radians(b[1])
    h = (
        math.sin((lat2 - lat1) / 2) ** 2
        + math.cos(lat1) * math.cos(lat2) * math.sin((lng2 - lng1) / 2) ** 2
    )
    return 2 * EARTH_RADIUS_M * math.asin(math.sqrt(h))


def in_box(lat, lng):
    return BOX[0] <= lat <= BOX[2] and BOX[1] <= lng <= BOX[3]


def chain_ways(ways, gap_max_m=GAP_MAX_M):
    """Chain ways (lists of (lat, lng)) in order into one polyline.

    Each way is flipped when its far end connects better. Returns (points, None)
    or (None, reason) when a gap larger than gap_max_m appears.
    """
    ways = [w for w in ways if len(w) >= 2]
    if not ways:
        return None, "no ways"
    first = list(ways[0])
    if len(ways) > 1:
        nxt = ways[1]
        end_cost = min(haversine_m(first[-1], nxt[0]), haversine_m(first[-1], nxt[-1]))
        start_cost = min(haversine_m(first[0], nxt[0]), haversine_m(first[0], nxt[-1]))
        if start_cost < end_cost:
            first.reverse()
    chain = first
    for way in ways[1:]:
        way = list(way)
        to_start = haversine_m(chain[-1], way[0])
        to_end = haversine_m(chain[-1], way[-1])
        if to_end < to_start:
            way.reverse()
            gap = to_end
        else:
            gap = to_start
        if gap > gap_max_m:
            return None, f"gap {gap:.0f} m"
        if gap == 0:
            chain.extend(way[1:])
        else:
            chain.extend(way)
    return chain, None


def cumulative_m(line):
    cum = [0.0]
    for i in range(1, len(line)):
        cum.append(cum[-1] + haversine_m(line[i - 1], line[i]))
    return cum


def project_on_line(line, cum, point):
    """Nearest point on the line: returns (along_m, off_line_m)."""
    lat0 = math.radians(point[0])
    kx = math.radians(1.0) * math.cos(lat0) * EARTH_RADIUS_M
    ky = math.radians(1.0) * EARTH_RADIUS_M
    best_dist = math.inf
    best_along = 0.0
    px, py = point[1], point[0]
    for i in range(len(line) - 1):
        ax = (line[i][1] - px) * kx
        ay = (line[i][0] - py) * ky
        bx = (line[i + 1][1] - px) * kx
        by = (line[i + 1][0] - py) * ky
        dx, dy = bx - ax, by - ay
        denom = dx * dx + dy * dy
        t = 0.0 if denom == 0 else max(0.0, min(1.0, -(ax * dx + ay * dy) / denom))
        dist = math.hypot(ax + t * dx, ay + t * dy)
        if dist < best_dist:
            best_dist = dist
            best_along = cum[i] + t * (cum[i + 1] - cum[i])
    return best_along, best_dist


def point_at(line, cum, along):
    if along <= 0:
        return line[0]
    if along >= cum[-1]:
        return line[-1]
    i = bisect.bisect_right(cum, along) - 1
    span = cum[i + 1] - cum[i]
    t = 0.0 if span == 0 else (along - cum[i]) / span
    return (
        line[i][0] + t * (line[i + 1][0] - line[i][0]),
        line[i][1] + t * (line[i + 1][1] - line[i][1]),
    )


def cut_line(line, cum, a, b):
    """Sub-polyline between along-distances a < b."""
    points = [point_at(line, cum, a)]
    for i, d in enumerate(cum):
        if a < d < b:
            points.append(line[i])
    points.append(point_at(line, cum, b))
    return points


def boundary_edges(ways):
    """Boundary ways -> list of ((lat, lng), (lat, lng)) edges for point_in_boundary."""
    edges = []
    for way in ways:
        edges.extend(zip(way, way[1:]))
    return edges


def point_in_boundary(point, edges):
    """Even-odd ray cast over all boundary edges (outer and inner rings alike)."""
    lat, lng = point
    inside = False
    for (lat1, lng1), (lat2, lng2) in edges:
        if (lng1 > lng) != (lng2 > lng):
            lat_cross = lat1 + (lng - lng1) / (lng2 - lng1) * (lat2 - lat1)
            if lat < lat_cross:
                inside = not inside
    return inside


def minutes_est(distance_m):
    return math.ceil(distance_m / M_PER_MIN)


# ---------------------------------------------------------------- routes


def route_stops(relation, node_tags):
    """Ordered named stop nodes inside the box: [(id, name, lat, lng)]."""
    members = [m for m in relation.get("members", []) if m.get("type") == "node"]

    def pick(role_test):
        out = []
        for m in members:
            if not role_test(m.get("role", "")):
                continue
            if "lat" not in m:
                continue
            name = (node_tags.get(m["ref"]) or {}).get("name", "").strip()
            if not name or not in_box(m["lat"], m["lon"]):
                continue
            out.append((m["ref"], name, m["lat"], m["lon"]))
        return out

    stops = pick(lambda r: r.startswith("stop"))
    if not stops:
        stops = pick(lambda r: r == "platform")
    return stops


def way_geometries(relation):
    ways = []
    for m in relation.get("members", []):
        if m.get("type") != "way" or m.get("role", "") not in ("", "forward", "backward"):
            continue
        geom = m.get("geometry") or []
        pts = [(g["lat"], g["lon"]) for g in geom if g]
        if len(pts) >= 2:
            ways.append(pts)
    return ways


def build_segments(line, stops, seg_min=SEGMENT_MIN_M, seg_max=SEGMENT_MAX_M):
    """Return (kept_stops, segments, drop_counts).

    kept_stops: stops <= 60 m from the line with strictly increasing along-line
    distance. segments: [(from_stop, to_stop, points, distance_m)] between
    consecutive kept stops, with seg_min <= distance <= seg_max.
    """
    cum = cumulative_m(line)
    drops = Counter()
    kept = []  # (stop, along)
    for stop in stops:
        along, off = project_on_line(line, cum, (stop[2], stop[3]))
        if off > STOP_OFF_LINE_MAX_M:
            drops["stop off line"] += 1
            continue
        if kept and along <= kept[-1][1]:
            drops["stop not increasing"] += 1
            continue
        kept.append((stop, along))
    segments = []
    for (s_from, a), (s_to, b) in zip(kept, kept[1:]):
        points = cut_line(line, cum, a, b)
        distance = round(sum(haversine_m(p, q) for p, q in zip(points, points[1:])))
        if distance < seg_min:
            drops["segment too short"] += 1
            continue
        if distance > seg_max:
            drops["segment too long"] += 1
            continue
        segments.append((s_from, s_to, points, distance))
    return [k[0] for k in kept], segments, drops


def route_mode(osm_route):
    return {"share_taxi": "jeepney", "minibus": "minibus"}.get(osm_route, "bus")


def signboards(tags):
    seen = []
    for key in ("ref", "to", "name"):
        value = (tags.get(key) or "").strip()
        if value and value not in seen:
            seen.append(value)
    return seen


def route_name(tags):
    name = (tags.get("name") or "").strip()
    if name:
        return name
    ref = (tags.get("ref") or "").strip()
    frm = (tags.get("from") or "").strip()
    to = (tags.get("to") or "").strip()
    span = f"{frm} – {to}" if frm and to else (frm or to)
    return " ".join(p for p in (ref, span) if p) or "OSM route"


def build_routes(elements, generated_at):
    relations = [e for e in elements if e["type"] == "relation"]
    node_tags = {e["id"]: e.get("tags", {}) for e in elements if e["type"] == "node"}
    stats = {"fetched": len(relations), "kept": 0, "skipped": Counter(), "dropped": Counter()}
    routes, segments, stops = [], [], {}
    for rel in sorted(relations, key=lambda r: r["id"]):
        tags = rel.get("tags", {})
        rel_id = f"osm-r{rel['id']}"
        line, reason = chain_ways(way_geometries(rel))
        if line is None:
            stats["skipped"]["gap > 60 m" if reason.startswith("gap") else reason] += 1
            continue
        named = route_stops(rel, node_tags)
        if len(named) < 2:
            stats["skipped"]["< 2 stops in box"] += 1
            continue
        kept, segs, drops = build_segments(line, named)
        stats["dropped"].update(drops)
        if len(kept) < 2:
            stats["skipped"]["< 2 stops kept on line"] += 1
            continue
        if not segs:
            stats["skipped"]["no segment within 50-6000 m"] += 1
            continue
        mode = route_mode(tags.get("route"))
        routes.append(
            {
                "id": rel_id,
                "name": route_name(tags),
                "mode": mode,
                "signboards": signboards(tags),
                "source_class": "osm",
            }
        )
        stop_mode = "jeepney" if mode == "jeepney" else "bus"
        for seq, (s_from, s_to, points, distance) in enumerate(segs, start=1):
            for s in (s_from, s_to):
                entry = stops.setdefault(
                    f"osm-n{s[0]}",
                    {
                        "id": f"osm-n{s[0]}",
                        "name": s[1],
                        "lat": s[2],
                        "lng": s[3],
                        "modes": [],
                    },
                )
                if stop_mode not in entry["modes"]:
                    entry["modes"].append(stop_mode)
            segments.append(
                {
                    "route_id": rel_id,
                    "seq": seq,
                    "from_stop_id": f"osm-n{s_from[0]}",
                    "to_stop_id": f"osm-n{s_to[0]}",
                    "fare_php": None,
                    "minutes": None,
                    "minutes_est": minutes_est(distance),
                    "distance_m": distance,
                    "shape": {
                        "polyline": encode_polyline(points, 6),
                        "engine": "osm-route-geometry",
                        "profile": None,
                        "generated_at": generated_at,
                    },
                    "provenance": {
                        "source_class": "osm",
                        "source": f"OpenStreetMap relation {rel['id']}",
                        "verified_by": None,
                        "verified_at": None,
                    },
                }
            )
        stats["kept"] += 1
    stop_list = sorted(stops.values(), key=lambda s: s["id"])
    return routes, stop_list, segments, stats


# ---------------------------------------------------------------- places

# Lower rank = kept first when over the cap.
_RANK = {
    "barangay": 0,
    "mall": 1,
    "department_store": 1,
    "station": 2,
    "hospital": 3,
    "university": 4,
    "college": 4,
    "park": 5,
}
# Pass 1 quota per priority group (rank above); the rest of PLACE_CAP fills by nearness.
GROUP_QUOTA = {0: 90, 1: 60, 2: 50, 3: 40, 4: 40, 5: 40}
CENTER = (14.5547, 121.0244)  # Ayala / Makati CBD
ALIAS_KEYS = ("name:en", "alt_name", "short_name", "official_name", "old_name", "brand", "name:tl")


def place_kind(tags):
    if tags.get("boundary") == "administrative" and tags.get("admin_level") == "10":
        return "barangay"
    shop = tags.get("shop")
    if shop in ("mall", "department_store"):
        return "mall"
    if tags.get("railway") == "station" or tags.get("public_transport") == "station":
        return "station"
    amenity = tags.get("amenity")
    if amenity == "bus_station":
        return "station"
    if amenity:
        return amenity
    if tags.get("leisure") == "park":
        return "park"
    if tags.get("tourism") in ("museum", "attraction"):
        return tags["tourism"]
    if tags.get("place"):
        return "area"
    return None


def place_aliases(tags):
    name = tags.get("name", "").strip()
    out = []
    for key in ALIAS_KEYS:
        for part in (tags.get(key) or "").split(";"):
            part = part.strip()
            if part and part != name and part not in out:
                out.append(part)
    return out


def place_center(element):
    if element["type"] == "node":
        return element.get("lat"), element.get("lon")
    center = element.get("center")
    if center:
        return center.get("lat"), center.get("lon")
    bounds = element.get("bounds")  # out geom relations: same as out center (bbox centre)
    if bounds:
        return (bounds["minlat"] + bounds["maxlat"]) / 2, (bounds["minlon"] + bounds["maxlon"]) / 2
    return None, None


def mostly_inside(element, edges, samples=40, share=0.5):
    """True when at least `share` of sampled boundary vertices of a relation lie inside edges.

    A relation's bbox centre can fall inside Makati while the area belongs to a neighbour,
    so barangays are tested by their own outline instead. The PSGC `ref` decides first when
    present: the Makati boundary polygon still contains edge areas of neighbouring barangays.
    """
    ref = (element.get("tags") or {}).get("ref", "")
    if ref:
        return ref.startswith(MAKATI_PSGC_PREFIX)
    vertices = [
        (g["lat"], g["lon"])
        for m in element.get("members", [])
        if m.get("type") == "way"
        for g in m.get("geometry") or []
        if g
    ]
    if not vertices:
        return False
    step = max(1, len(vertices) // samples)
    picked = vertices[::step]
    inside = sum(1 for v in picked if point_in_boundary(v, edges))
    return inside / len(picked) >= share


def build_places(elements, cap=PLACE_CAP, boundary=None):
    candidates = []
    for el in elements:
        tags = el.get("tags") or {}
        name = (tags.get("name") or "").strip()
        if not name or name.replace(" ", "").isdigit():
            continue
        kind = place_kind(tags)
        lat, lng = place_center(el)
        if kind is None or lat is None or not in_box(lat, lng):
            continue
        if boundary is not None:
            if kind == "barangay":
                if not mostly_inside(el, boundary):
                    continue
            elif not point_in_boundary((lat, lng), boundary):
                continue
        if kind == "mall" and el["type"] == "node":
            # shop=mall on a node is an individual store (Muji, H&M Home), not a mall site
            continue
        candidates.append(
            {
                "id": f"osm-{el['type'][0]}{el['id']}",
                "name": name,
                "aliases": place_aliases(tags),
                "lat": round(lat, 6),
                "lng": round(lng, 6),
                "kind": kind,
                "stop_ids": [],
                "source_class": "osm",
                "_rank": _RANK.get(kind if kind != "area" else "", 6),
            }
        )
    candidates.sort(key=lambda p: (p["_rank"], p["name"].lower(), -len(p["aliases"]), p["id"]))
    # Same name and kind within 300 m is one place seen as node + way, etc.
    chosen = []
    for cand in candidates:
        if any(
            c["kind"] == cand["kind"]
            and c["name"].lower() == cand["name"].lower()
            and haversine_m((c["lat"], c["lng"]), (cand["lat"], cand["lng"])) < 300
            for c in chosen
        ):
            continue
        chosen.append(cand)
    total = len(chosen)
    for c in chosen:
        c["_dist"] = haversine_m((c["lat"], c["lng"]), CENTER)
    chosen.sort(key=lambda p: (p["_rank"], p["_dist"], p["id"]))
    # Pass 1: each priority group gets up to its quota, nearest the Makati centre first,
    # so barangays of neighbouring cities at the box edge cannot crowd out everything else.
    taken, left = [], []
    per_group = Counter()
    for c in chosen:
        if per_group[c["_rank"]] < GROUP_QUOTA.get(c["_rank"], 0):
            per_group[c["_rank"]] += 1
            taken.append(c)
        else:
            left.append(c)
    # Pass 2: remaining slots go to the nearest leftovers of any kind.
    left.sort(key=lambda p: (p["_dist"], p["id"]))
    taken.extend(left[: max(0, cap - len(taken))])
    chosen = sorted(taken[:cap], key=lambda p: (p["_rank"], p["_dist"], p["id"]))
    for c in chosen:
        del c["_rank"], c["_dist"]
    return chosen, total


# ---------------------------------------------------------------- sanity


def check_pack(pack):
    stop_by_id = {s["id"]: s for s in pack["stops"]}
    route_ids = {r["id"] for r in pack["routes"]}
    if len(stop_by_id) != len(pack["stops"]):
        raise OsmImportError("duplicate stop ids")
    if len(route_ids) != len(pack["routes"]):
        raise OsmImportError("duplicate route ids")
    if len({p["id"] for p in pack["places"]}) != len(pack["places"]):
        raise OsmImportError("duplicate place ids")
    for s in pack["stops"]:
        if not in_box(s["lat"], s["lng"]):
            raise OsmImportError(f"stop outside box: {s['id']}")
    for p in pack["places"]:
        if not in_box(p["lat"], p["lng"]):
            raise OsmImportError(f"place outside box: {p['id']}")
    seen = set()
    for seg in pack["segments"]:
        key = (seg["route_id"], seg["seq"])
        if key in seen:
            raise OsmImportError(f"duplicate segment {key}")
        seen.add(key)
        if seg["route_id"] not in route_ids:
            raise OsmImportError(f"segment route missing: {seg['route_id']}")
        a, b = stop_by_id.get(seg["from_stop_id"]), stop_by_id.get(seg["to_stop_id"])
        if a is None or b is None:
            raise OsmImportError(f"segment stop missing: {key}")
        points = decode_polyline(seg["shape"]["polyline"], 6)
        if len(points) < 2:
            raise OsmImportError(f"shape too short: {key}")
        if haversine_m(points[0], (a["lat"], a["lng"])) > 60:
            raise OsmImportError(f"shape start > 60 m from from-stop: {key}")
        if haversine_m(points[-1], (b["lat"], b["lng"])) > 60:
            raise OsmImportError(f"shape end > 60 m from to-stop: {key}")
        if seg["fare_php"] is not None or seg["minutes"] is not None:
            raise OsmImportError(f"OSM segment has fare/minutes: {key}")
    for r in pack["routes"]:
        if not any(seg["route_id"] == r["id"] for seg in pack["segments"]):
            raise OsmImportError(f"route without segments: {r['id']}")


# ---------------------------------------------------------------- fetch


def overpass(query, label):
    body = urllib.parse.urlencode({"data": query}).encode()
    last = None
    for url in OVERPASS_URLS:
        req = urllib.request.Request(
            url, data=body, headers={"User-Agent": USER_AGENT, "Accept": "application/json"}
        )
        try:
            print(f"[{label}] POST {url}", file=sys.stderr)
            with urllib.request.urlopen(req, timeout=300) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            if "elements" not in data:
                raise OsmImportError("response has no elements")
            if data.get("remark") and "error" in data["remark"].lower():
                raise OsmImportError(f"overpass remark: {data['remark']}")
            return data
        except (urllib.error.URLError, OSError, ValueError, OsmImportError) as exc:
            last = exc
            print(f"[{label}] failed on {url}: {exc}", file=sys.stderr)
            time.sleep(2)
    raise OsmImportError(f"all Overpass endpoints failed for {label}: {last}")


def fetch_cached(cache_dir, name, query, refresh):
    path = os.path.join(cache_dir, name)
    if os.path.exists(path) and not refresh:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    data = overpass(query, name)
    os.makedirs(cache_dir, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False)
    time.sleep(5)  # stay under the Overpass rate limit between requests
    return data


def bbox_str():
    return ",".join(str(v) for v in BOX)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--cache-dir", default=DEFAULT_CACHE)
    ap.add_argument("--refresh", action="store_true", help="ignore cached Overpass responses")
    args = ap.parse_args(argv)

    now = datetime.now(timezone.utc)
    stamp = now.strftime("%Y-%m-%dT%H:%M:%SZ")
    routes_raw = fetch_cached(
        args.cache_dir, "osm-routes.json", ROUTES_QUERY.format(bbox=bbox_str()), args.refresh
    )
    places_raw = fetch_cached(
        args.cache_dir,
        "osm-places.json",
        PLACES_QUERY.format(bbox=bbox_str(), amenities=AMENITIES),
        args.refresh,
    )

    barangays_raw = fetch_cached(
        args.cache_dir,
        "osm-barangays.json",
        BARANGAYS_QUERY.format(bbox=bbox_str()),
        args.refresh,
    )
    boundary_raw = fetch_cached(
        args.cache_dir,
        "osm-makati-boundary.json",
        BOUNDARY_QUERY.format(relation_id=MAKATI_RELATION_ID),
        args.refresh,
    )
    boundary = boundary_edges(
        [
            [(g["lat"], g["lon"]) for g in el["geometry"]]
            for el in boundary_raw["elements"]
            if el["type"] == "way"
        ]
    )
    if not boundary:
        raise OsmImportError("Makati boundary has no ways")

    routes, stops, segments, stats = build_routes(routes_raw["elements"], stamp)
    places, places_total = build_places(
        places_raw["elements"] + barangays_raw["elements"], boundary=boundary
    )
    pack = {
        "meta": {
            "version": "0.1.0-osm",
            "built_at": stamp,
            "coverage": {"name": "Makati City (OSM import)"},
            "sources": [
                f"© OpenStreetMap contributors (ODbL), via Overpass API, fetched {now.date().isoformat()}"
            ],
            "shape_precision": 6,
            "minutes_est_m_per_min": M_PER_MIN,
            "notes": (
                "OSM-only import, source_class osm: real open data, not team-verified. "
                "fare_php is null (unknown). minutes is null; minutes_est = ceil(distance_m / 120). "
                "Stops and routes: coverage box lat 14.525-14.585, lng 120.995-121.065 (includes edges of neighbouring cities). "
                f"Places: only inside the Makati City boundary relation {MAKATI_RELATION_ID} (admin_level 6)."
            ),
        },
        "places": places,
        "stops": stops,
        "routes": sorted(routes, key=lambda r: r["id"]),
        "segments": segments,
        "transfers": [],
    }
    check_pack(pack)

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=1)
        f.write("\n")

    kinds = Counter(p["kind"] for p in places)
    print(f"relations fetched: {stats['fetched']}")
    print(f"relations kept:    {stats['kept']}")
    print(f"relations skipped: {sum(stats['skipped'].values())}")
    for reason, n in stats["skipped"].most_common():
        print(f"  {reason}: {n}")
    if stats["dropped"]:
        print("dropped inside kept relations:")
        for reason, n in stats["dropped"].most_common():
            print(f"  {reason}: {n}")
    print(f"stops:    {len(stops)}")
    print(f"segments: {len(segments)}")
    print(f"places:   {len(places)} (of {places_total} candidates, cap {PLACE_CAP})")
    for kind, n in kinds.most_common():
        print(f"  {kind}: {n}")
    print(f"wrote {args.out} ({os.path.getsize(args.out) / 1024:.0f} KiB)")


if __name__ == "__main__":
    try:
        main()
    except OsmImportError as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        sys.exit(1)
