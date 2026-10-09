import json
import math
import os
import time

EARTH_RADIUS_M = 6_371_000


def _encode_signed(value: int) -> str:
    value = ~(value << 1) if value < 0 else (value << 1)
    chars = []
    while value >= 0x20:
        chars.append(chr((0x20 | (value & 0x1F)) + 63))
        value >>= 5
    chars.append(chr(value + 63))
    return "".join(chars)


def encode_polyline(points: list[tuple[float, float]], precision: int = 6) -> str:
    factor = 10 ** precision
    encoded = []
    last_lat = 0
    last_lng = 0
    for lat, lng in points:
        ilat = int(round(lat * factor))
        ilng = int(round(lng * factor))
        encoded.append(_encode_signed(ilat - last_lat))
        encoded.append(_encode_signed(ilng - last_lng))
        last_lat = ilat
        last_lng = ilng
    return "".join(encoded)


def decode_polyline(encoded: str, precision: int = 6) -> list[tuple[float, float]]:
    factor = 10 ** precision
    points: list[tuple[float, float]] = []
    index = 0
    lat = 0
    lng = 0
    length = len(encoded)

    def _next_signed() -> int:
        nonlocal index
        result = 0
        shift = 0
        while True:
            b = ord(encoded[index]) - 63
            index += 1
            result |= (b & 0x1F) << shift
            shift += 5
            if b < 0x20:
                break
        return ~(result >> 1) if result & 1 else (result >> 1)

    while index < length:
        lat += _next_signed()
        lng += _next_signed()
        points.append((lat / factor, lng / factor))
    return points


def length_m(points: list[tuple[float, float]]) -> int:
    total = 0.0
    for (lat1, lng1), (lat2, lng2) in zip(points, points[1:]):
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        dphi = math.radians(lat2 - lat1)
        dlambda = math.radians(lng2 - lng1)
        a = (
            math.sin(dphi / 2) ** 2
            + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
        )
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        total += EARTH_RADIUS_M * c
    return round(total)


SNAP_M = 30
EXTRA_M = 40
_STRAIGHT_M = 5


class ShapeError(Exception):
    pass


def _distance_m(a: tuple[float, float], b: tuple[float, float]) -> float:
    (lat1, lng1), (lat2, lng2) = a, b
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lng2 - lng1)
    x = (
        math.sin(dphi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(x), math.sqrt(1 - x))
    return EARTH_RADIUS_M * c


def _chord_distance_m(
    point: tuple[float, float],
    start: tuple[float, float],
    end: tuple[float, float],
) -> float:
    lat0 = (start[0] + end[0]) / 2
    scale_x = EARTH_RADIUS_M * math.cos(math.radians(lat0))

    def _xy(lat: float, lng: float) -> tuple[float, float]:
        return math.radians(lng) * scale_x, math.radians(lat) * EARTH_RADIUS_M

    px, py = _xy(point[0], point[1])
    ax, ay = _xy(start[0], start[1])
    bx, by = _xy(end[0], end[1])
    dx = bx - ax
    dy = by - ay
    length2 = dx * dx + dy * dy
    if length2 == 0:
        return _distance_m(point, start)
    t = ((px - ax) * dx + (py - ay) * dy) / length2
    t = max(0.0, min(1.0, t))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def trim_overshoot(
    points: list[tuple[float, float]],
    end: tuple[float, float],
    snap_m: int = SNAP_M,
    extra_m: int = EXTRA_M,
) -> list[tuple[float, float]]:
    close_idx = None
    close_dist = None
    for i, point in enumerate(points):
        dist = _distance_m(point, end)
        if close_idx is None:
            if dist <= snap_m:
                close_idx = i
                close_dist = dist
            continue
        if dist > close_dist + extra_m:
            return list(points[: close_idx + 1])
    return points


def check_segment(
    route_id: str,
    points: list[tuple[float, float]],
    start: tuple[float, float],
    end: tuple[float, float],
    distance_m: int,
) -> None:
    def _fail(detail: str) -> None:
        raise ShapeError(f"{route_id}: {detail}")

    if len(points) < 3:
        _fail("missing or too few points")
    if _distance_m(points[0], start) > SNAP_M or _distance_m(points[-1], end) > SNAP_M:
        _fail("endpoint farther than snap from stop")
    first, last = points[0], points[-1]
    if all(_chord_distance_m(mid, first, last) <= _STRAIGHT_M for mid in points[1:-1]):
        _fail("straight-line shape")
    if abs(distance_m - length_m(points)) > 1:
        _fail("distance_m does not match length")


def fill_hero(source_path: str, cache_path: str, fetch) -> None:
    # shortcut: car profile and this one hero file only. A via list is the next step if the trimmed end is farther than 30 m from the stop.
    if os.path.exists(cache_path):
        with open(cache_path, encoding="utf-8") as cached:
            body = cached.read()
    else:
        body = fetch()
        cache_dir = os.path.dirname(cache_path)
        if cache_dir:
            os.makedirs(cache_dir, exist_ok=True)
        with open(cache_path, "w", encoding="utf-8") as cached:
            cached.write(body)
        with open(cache_path, encoding="utf-8") as cached:
            body = cached.read()

    geometry = json.loads(body)["routes"][0]["geometry"]
    points = decode_polyline(geometry, 6)

    with open(source_path, encoding="utf-8") as source_file:
        source = json.load(source_file)

    stops = {stop["id"]: stop for stop in source["stops"]}
    end_stop = stops[source["segments"][0]["to_stop_id"]]
    end = (end_stop["lat"], end_stop["lng"])
    trimmed = trim_overshoot(points, end)
    if _distance_m(trimmed[-1], end) > SNAP_M:
        raise ShapeError("BLOCKED: trimmed end farther than 30 m from the get-off stop")

    polyline = encode_polyline(trimmed, 6)
    distance = length_m(trimmed)
    generated_at = None
    for segment in source["segments"]:
        existing = segment.get("shape") or {}
        if isinstance(existing, dict) and existing.get("generated_at"):
            generated_at = existing["generated_at"]
            break
    if not generated_at:
        generated_at = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(os.path.getmtime(cache_path)))
    shape = {
        "polyline": polyline,
        "engine": "osrm",
        "profile": "driving",
        "generated_at": generated_at,
    }
    for segment in source["segments"]:
        segment["shape"] = shape
        segment["distance_m"] = distance

    credit_prefix = "OSRM demo server, driving profile, polyline6"
    if not any(line.startswith(credit_prefix) for line in source["meta"]["sources"]):
        source["meta"]["sources"].append(
            f"{credit_prefix}, fetched {generated_at}. OpenStreetMap ODbL."
        )
    source["meta"]["notes"] = (
        "Source file for the hero trip only (state.md D37). "
        "Road shapes and distance_m come from the cached OSRM driving line."
    )

    with open(source_path, "w", encoding="utf-8") as source_file:
        json.dump(source, source_file, indent=2, ensure_ascii=False)
        source_file.write("\n")

    geojson_path = os.path.join(os.path.dirname(source_path) or ".", "hero-ride.geojson")
    geojson = {
        "type": "LineString",
        "coordinates": [[lng, lat] for lat, lng in trimmed],
    }
    with open(geojson_path, "w", encoding="utf-8") as geojson_file:
        json.dump(geojson, geojson_file, indent=2)
        geojson_file.write("\n")
