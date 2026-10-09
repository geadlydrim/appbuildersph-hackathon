"""Build data/pack/carousel-makati.source.json: the EDSA Carousel stops inside Makati.

OSM's Carousel relations skip the Ayala stop (Guadalupe -> Buendia 1942 m -> Tramo 4170 m),
so this script takes the OSM southbound line, cuts it at One Ayala, and writes the three
operating Makati stops (Guadalupe, Buendia, Ayala) as their own pack file.

  python carousel.py                       build carousel-makati.source.json from osm-makati.source.json
  python carousel.py --strip-osm-carousel  one-off: remove the two OSM Carousel routes from osm-makati.source.json

Run the build first, then the strip: the build needs the OSM geometry the strip removes.
osm_import.py skips these relations (EXCLUDED_RELATIONS), so a re-import stays consistent.
Stdlib only.
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone

from osm_import import (
    EXCLUDED_RELATIONS,
    OsmImportError,
    M_PER_MIN,
    check_pack,
    cumulative_m,
    cut_line,
    haversine_m,
    minutes_est,
    project_on_line,
)
from shapes import decode_polyline, encode_polyline

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OSM = os.path.join(HERE, "osm-makati.source.json")
DEFAULT_OUT = os.path.join(HERE, "carousel-makati.source.json")

# OSM southbound relation (-> PITX) and the stop nodes / place the pack is built from.
SB_RELATION = 10183711
OSM_GUADALUPE_NODE = "osm-n7666737324"
OSM_BUENDIA_NODE = "osm-n6542649635"
OSM_ONE_AYALA_PLACE = "osm-w1117016729"
AYALA_OFF_LINE_MAX_M = 150
TOTAL_KM_RANGE = (2.5, 3.5)  # Wikipedia's cumulative km (15.80 -> 18.80) give 3.0 km
SEGMENT_SOURCE = (
    "EDSA Carousel stops: Wikipedia stops table (accessed 2026-10-10); "
    f"geometry: OpenStreetMap relation {SB_RELATION}"
)


class CarouselError(Exception):
    pass


def line_length_m(points):
    return sum(haversine_m(p, q) for p, q in zip(points, points[1:]))


def reverse_line(points):
    return list(reversed(points))


def cut_at_point(line, point, off_max_m=AYALA_OFF_LINE_MAX_M):
    """Split a line at the projection of `point`: returns (before, after, off_line_m).

    Fails loudly when the point is more than off_max_m from the line or projects onto an end.
    """
    cum = cumulative_m(line)
    along, off = project_on_line(line, cum, point)
    if off > off_max_m:
        raise CarouselError(f"point projects {off:.0f} m off the line (max {off_max_m} m)")
    if not 0 < along < cum[-1]:
        raise CarouselError("point projects onto an end of the line, cannot cut")
    return cut_line(line, cum, 0, along), cut_line(line, cum, along, cum[-1]), off


def _find(items, item_id, what):
    for item in items:
        if item["id"] == item_id:
            return item
    raise CarouselError(f"{what} {item_id} not found in OSM pack")


def osm_sb_lines(osm):
    """The OSM southbound Guadalupe->Buendia and Buendia->Tramo polylines (lists of points)."""
    rel_id = f"osm-r{SB_RELATION}"
    segs = sorted(
        (s for s in osm["segments"] if s["route_id"] == rel_id), key=lambda s: s["seq"]
    )
    if not segs:
        raise CarouselError(
            f"{rel_id} is not in the OSM pack (already stripped?). "
            "Point --osm at a copy that still has it, e.g. from git history."
        )
    if (segs[0]["from_stop_id"], segs[0]["to_stop_id"]) != (OSM_GUADALUPE_NODE, OSM_BUENDIA_NODE):
        raise CarouselError(f"{rel_id} seq 1 is not Guadalupe -> Buendia")
    if segs[1]["from_stop_id"] != OSM_BUENDIA_NODE:
        raise CarouselError(f"{rel_id} seq 2 does not start at Buendia")
    return (
        decode_polyline(segs[0]["shape"]["polyline"], 6),
        decode_polyline(segs[1]["shape"]["polyline"], 6),
    )


def build_pack(osm, built_at):
    """Return (pack, distances) where distances maps segment label -> metres."""
    guad = _find(osm["stops"], OSM_GUADALUPE_NODE, "stop")
    buen = _find(osm["stops"], OSM_BUENDIA_NODE, "stop")
    ayala_place = _find(osm["places"], OSM_ONE_AYALA_PLACE, "place")
    one_ayala = (ayala_place["lat"], ayala_place["lng"])

    line_gb, line_bt = osm_sb_lines(osm)
    try:
        line_ba, _rest, off = cut_at_point(line_bt, one_ayala)
    except CarouselError as exc:
        raise CarouselError(f"One Ayala: {exc}") from exc

    stops = [
        {
            "id": "carousel-guadalupe",
            "name": "Guadalupe (EDSA Carousel)",
            "lat": guad["lat"],
            "lng": guad["lng"],
            "modes": ["bus"],
            "notes": "On Guadalupe Bridge, Mandaluyong-Makati boundary. Position: OSM Carousel stop node.",
        },
        {
            "id": "carousel-buendia",
            "name": "Buendia (EDSA Carousel)",
            "lat": buen["lat"],
            "lng": buen["lng"],
            "modes": ["bus"],
            "notes": "Makati, EDSA median. Position: OSM Carousel stop node.",
        },
        {
            "id": "carousel-ayala",
            "name": "Ayala / One Ayala (EDSA Carousel)",
            "lat": one_ayala[0],
            "lng": one_ayala[1],
            "modes": ["bus"],
            "notes": (
                "Southbound boards inside One Ayala; northbound at the curbside. "
                f"Position: OSM One Ayala ({OSM_ONE_AYALA_PLACE}), {off:.0f} m from the OSM Carousel line."
            ),
        },
    ]

    sb_lines = [line_gb, line_ba]
    sb_stops = ["carousel-guadalupe", "carousel-buendia", "carousel-ayala"]
    nb_lines = [reverse_line(line_ba), reverse_line(line_gb)]
    nb_stops = list(reversed(sb_stops))

    segments, distances = [], {}
    names = {s["id"]: s["name"].split(" (")[0] for s in stops}
    for route_id, route_stops, lines in (
        ("carousel-sb", sb_stops, sb_lines),
        ("carousel-nb", nb_stops, nb_lines),
    ):
        for seq, (line, a, b) in enumerate(zip(lines, route_stops, route_stops[1:]), start=1):
            polyline = encode_polyline(line, 6)
            distance = round(line_length_m(decode_polyline(polyline, 6)))
            distances[f"{route_id} {names[a]} -> {names[b]}"] = distance
            segments.append(
                {
                    "route_id": route_id,
                    "seq": seq,
                    "from_stop_id": a,
                    "to_stop_id": b,
                    "fare_php": None,
                    "minutes": None,
                    "minutes_est": minutes_est(distance),
                    "distance_m": distance,
                    "shape": {
                        "polyline": polyline,
                        "engine": "osm-route-geometry",
                        "profile": None,
                        "generated_at": built_at,
                    },
                    "provenance": {
                        "source_class": "public",
                        "source": SEGMENT_SOURCE,
                        "verified_by": None,
                        "verified_at": None,
                    },
                }
            )
    check_total(sum(s["distance_m"] for s in segments if s["route_id"] == "carousel-sb"))

    routes = [
        {
            "id": "carousel-sb",
            "name": "EDSA Carousel (southbound)",
            "mode": "bus",
            "signboards": ["EDSA Carousel", "Route 1", "PITX"],
            "fare_rule": "bus_aircon",
            "source_class": "public",
        },
        {
            "id": "carousel-nb",
            "name": "EDSA Carousel (northbound)",
            "mode": "bus",
            "signboards": ["EDSA Carousel", "Route 1", "Monumento"],
            "fare_rule": "bus_aircon",
            "source_class": "public",
        },
    ]
    places = [
        {
            "id": f"carousel-place-{key}",
            "name": f"EDSA Carousel {label}",
            "aliases": aliases,
            "lat": stop["lat"],
            "lng": stop["lng"],
            "kind": "station",
            "stop_ids": [stop["id"]],
            "source_class": "public",
        }
        for key, label, aliases, stop in (
            ("guadalupe", "Guadalupe", ["Carousel Guadalupe", "Guadalupe Carousel"], stops[0]),
            ("buendia", "Buendia", ["Carousel Buendia", "Buendia Carousel"], stops[1]),
            (
                "ayala",
                "Ayala",
                ["Carousel Ayala", "Ayala Carousel", "One Ayala Carousel"],
                stops[2],
            ),
        )
    ]
    pack = {
        "meta": {
            "version": "0.1.0-carousel",
            "built_at": built_at,
            "coverage": {"name": "EDSA Carousel stops in Makati"},
            "sources": [
                "Wikipedia, \"EDSA Carousel\" stops table, accessed 2026-10-10",
                "© OpenStreetMap contributors (ODbL): Carousel relation "
                f"{SB_RELATION} geometry and stop/place coordinates, via Overpass API, fetched 2026-10-09",
            ],
            "shape_precision": 6,
            "minutes_est_m_per_min": M_PER_MIN,
            "notes": (
                "Operating Carousel stops in/at Makati: Guadalupe (Guadalupe Bridge, Mandaluyong-Makati boundary, "
                "15.80 km from Monumento), Buendia (EDSA median), Ayala / One Ayala (18.80 km; southbound boards "
                "inside One Ayala, northbound at the curbside). Magallanes and Estrella are not open; Tramo is "
                "southbound-only and in Pasay, so it is left out. Not ridden by the team: source_class public. "
                "minutes is null; minutes_est = ceil(distance_m / 120). "
                "Geometry: OSM southbound line, Buendia -> Tramo cut at One Ayala; northbound is the reversed line. "
                "fare_php is null: routes carry fare_rule bus_aircon, so the fare is computed per ride "
                "(consecutive segments of one route) from the ride's total distance_m using the owner's fare table "
                "supplied 2026-10-10: air-conditioned bus 18 PHP for the first 5 km plus 2.98 PHP per succeeding km."
            ),
        },
        "places": places,
        "stops": stops,
        "routes": routes,
        "segments": segments,
        "transfers": [],
    }
    check_pack(pack)
    return pack, distances


def check_total(sb_total_m):
    lo, hi = TOTAL_KM_RANGE
    if not lo * 1000 <= sb_total_m <= hi * 1000:
        raise CarouselError(
            f"Guadalupe -> Ayala is {sb_total_m} m, expected {lo}-{hi} km"
        )


def strip_osm_carousel(osm):
    """Remove the Carousel routes, their segments, and stops left without a segment.

    Mutates `osm`; returns (routes_removed, segments_removed, stops_removed).
    """
    drop = {f"osm-r{r}" for r in EXCLUDED_RELATIONS}
    routes = [r for r in osm["routes"] if r["id"] not in drop]
    segments = [s for s in osm["segments"] if s["route_id"] not in drop]
    used = {s["from_stop_id"] for s in segments} | {s["to_stop_id"] for s in segments}
    stops = [s for s in osm["stops"] if s["id"] in used]
    removed = (
        len(osm["routes"]) - len(routes),
        len(osm["segments"]) - len(segments),
        len(osm["stops"]) - len(stops),
    )
    osm["routes"], osm["segments"], osm["stops"] = routes, segments, stops
    return removed


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def dump(pack, path):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=1)
        f.write("\n")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--osm", default=DEFAULT_OSM, help="OSM pack to read geometry from / strip")
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument(
        "--strip-osm-carousel",
        action="store_true",
        help="remove the OSM Carousel routes from the OSM pack instead of building",
    )
    args = ap.parse_args(argv)

    osm = load(args.osm)
    if args.strip_osm_carousel:
        routes, segments, stops = strip_osm_carousel(osm)
        check_pack(osm)
        dump(osm, args.osm)
        print(f"removed from {os.path.basename(args.osm)}: {routes} routes, {segments} segments, {stops} stops")
        return

    built_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    pack, distances = build_pack(osm, built_at)
    dump(pack, args.out)
    for label, metres in distances.items():
        print(f"{label}: {metres} m")
    sb = sum(s["distance_m"] for s in pack["segments"] if s["route_id"] == "carousel-sb")
    print(f"Guadalupe -> Ayala total: {sb} m ({sb / 1000:.2f} km)")
    print(f"wrote {args.out} ({os.path.getsize(args.out) / 1024:.1f} KiB)")


if __name__ == "__main__":
    try:
        main()
    except (CarouselError, OsmImportError) as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        sys.exit(1)
