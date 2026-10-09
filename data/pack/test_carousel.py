import copy
import unittest

from carousel import (
    CarouselError,
    build_pack,
    check_total,
    cut_at_point,
    line_length_m,
    reverse_line,
    strip_osm_carousel,
)
from osm_import import haversine_m
from shapes import encode_polyline

# Synthetic EDSA: a straight north-south line; 0.001 deg lat is ~111 m.
LNG = 121.03


def lat_line(*lats):
    return [(lat, LNG) for lat in lats]


GUAD = (14.5725, LNG)
BUEN = (14.555, LNG)
TRAMO = (14.538, LNG)
AYALA = (14.5465, LNG + 0.0001)  # ~11 m east of the line, between Buendia and Tramo


def osm_pack(ayala=AYALA):
    def seg(route, seq, a, b, line):
        return {
            "route_id": route,
            "seq": seq,
            "from_stop_id": a,
            "to_stop_id": b,
            "fare_php": None,
            "minutes": None,
            "shape": {"polyline": encode_polyline(line, 6)},
        }

    def stop(sid, p):
        return {"id": sid, "name": sid, "lat": p[0], "lng": p[1], "modes": ["bus"]}

    gb = lat_line(GUAD[0], 14.564, BUEN[0])
    bt = lat_line(BUEN[0], 14.545, TRAMO[0])
    return {
        "places": [
            {"id": "osm-w1117016729", "name": "One Ayala", "lat": ayala[0], "lng": ayala[1]},
        ],
        "stops": [
            stop("osm-n7666737324", GUAD),
            stop("osm-n6542649635", BUEN),
            stop("osm-n10303501517", TRAMO),
            stop("osm-n1", (14.50, LNG)),  # used by another route only
            stop("osm-n2", (14.51, LNG)),
        ],
        "routes": [
            {"id": "osm-r10183711"},
            {"id": "osm-r9453755"},
            {"id": "osm-r1"},
        ],
        "segments": [
            seg("osm-r10183711", 1, "osm-n7666737324", "osm-n6542649635", gb),
            seg("osm-r10183711", 2, "osm-n6542649635", "osm-n10303501517", bt),
            seg("osm-r9453755", 1, "osm-n10303501517", "osm-n6542649635", reverse_line(bt)),
            seg("osm-r1", 1, "osm-n1", "osm-n2", lat_line(14.50, 14.51)),
            seg("osm-r1", 2, "osm-n2", "osm-n10303501517", lat_line(14.51, 14.538)),
        ],
    }


class CutTest(unittest.TestCase):
    def test_cut_at_projection_of_point(self):
        line = lat_line(14.555, 14.545, 14.538)
        before, after, off = cut_at_point(line, AYALA)
        self.assertAlmostEqual(off, 11, delta=2)
        self.assertAlmostEqual(before[-1][0], AYALA[0], places=5)
        self.assertEqual(before[-1], after[0])
        self.assertEqual(before[0], line[0])
        self.assertEqual(after[-1], line[-1])
        self.assertAlmostEqual(
            line_length_m(before) + line_length_m(after), line_length_m(line), delta=0.5
        )
        self.assertAlmostEqual(line_length_m(before), haversine_m(line[0], (AYALA[0], LNG)), delta=1)

    def test_too_far_off_line_fails(self):
        with self.assertRaises(CarouselError):
            cut_at_point(lat_line(14.555, 14.538), (14.5465, LNG + 0.003))  # ~320 m east

    def test_projection_on_end_fails(self):
        with self.assertRaises(CarouselError):
            cut_at_point(lat_line(14.555, 14.538), (14.56, LNG))  # beyond the start


class ReverseTest(unittest.TestCase):
    def test_reverse_line(self):
        line = lat_line(14.55, 14.54, 14.53)
        self.assertEqual(reverse_line(line), list(reversed(line)))
        self.assertEqual(line_length_m(reverse_line(line)), line_length_m(line))


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.pack, self.distances = build_pack(osm_pack(), "2026-10-10T00:00:00Z")
        self.segs = {(s["route_id"], s["seq"]): s for s in self.pack["segments"]}

    def test_segments_and_reversal(self):
        self.assertEqual(set(self.segs), {("carousel-sb", 1), ("carousel-sb", 2), ("carousel-nb", 1), ("carousel-nb", 2)})
        self.assertEqual(
            [(s["from_stop_id"], s["to_stop_id"]) for s in self.pack["segments"]],
            [
                ("carousel-guadalupe", "carousel-buendia"),
                ("carousel-buendia", "carousel-ayala"),
                ("carousel-ayala", "carousel-buendia"),
                ("carousel-buendia", "carousel-guadalupe"),
            ],
        )
        self.assertEqual(self.segs[("carousel-sb", 1)]["distance_m"], self.segs[("carousel-nb", 2)]["distance_m"])
        self.assertEqual(self.segs[("carousel-sb", 2)]["distance_m"], self.segs[("carousel-nb", 1)]["distance_m"])

    def test_segment_fields(self):
        for s in self.pack["segments"]:
            self.assertIsNone(s["fare_php"])
            self.assertIsNone(s["minutes"])
            self.assertEqual(s["minutes_est"], -(-s["distance_m"] // 120))
            self.assertEqual(s["provenance"]["source_class"], "public")
            self.assertEqual(s["shape"]["engine"], "osm-route-geometry")

    def test_stops_routes_places(self):
        self.assertEqual([s["id"] for s in self.pack["stops"]], ["carousel-guadalupe", "carousel-buendia", "carousel-ayala"])
        ayala = self.pack["stops"][2]
        self.assertEqual((ayala["lat"], ayala["lng"]), AYALA)
        self.assertEqual({r["id"]: r["fare_rule"] for r in self.pack["routes"]}, {"carousel-sb": "bus_aircon", "carousel-nb": "bus_aircon"})
        self.assertEqual(
            [p["stop_ids"] for p in self.pack["places"]],
            [["carousel-guadalupe"], ["carousel-buendia"], ["carousel-ayala"]],
        )

    def test_ayala_off_line_fails(self):
        with self.assertRaises(CarouselError):
            build_pack(osm_pack(ayala=(14.5465, LNG + 0.003)), "2026-10-10T00:00:00Z")

    def test_missing_carousel_route_fails(self):
        osm = osm_pack()
        osm["segments"] = [s for s in osm["segments"] if s["route_id"] != "osm-r10183711"]
        with self.assertRaises(CarouselError):
            build_pack(osm, "2026-10-10T00:00:00Z")


class TotalTest(unittest.TestCase):
    def test_range(self):
        check_total(2880)
        check_total(2500)
        check_total(3500)
        for bad in (2499, 3501, 6100):
            with self.assertRaises(CarouselError):
                check_total(bad)

    def test_out_of_range_total_fails_build(self):
        osm = osm_pack()
        # Stretch Guadalupe north by ~2 km so the total leaves 2.5-3.5 km.
        for seg in osm["segments"]:
            if (seg["route_id"], seg["seq"]) == ("osm-r10183711", 1):
                seg["shape"]["polyline"] = encode_polyline(lat_line(14.60, 14.555), 6)
        with self.assertRaises(CarouselError):
            build_pack(osm, "2026-10-10T00:00:00Z")


class StripTest(unittest.TestCase):
    def test_removes_only_carousel(self):
        osm = osm_pack()
        before = copy.deepcopy(osm)
        removed = strip_osm_carousel(osm)
        self.assertEqual(removed, (2, 3, 2))  # guadalupe + buendia; tramo is still used by osm-r1
        self.assertEqual([r["id"] for r in osm["routes"]], ["osm-r1"])
        self.assertEqual(osm["segments"], before["segments"][3:])
        self.assertEqual([s["id"] for s in osm["stops"]], ["osm-n10303501517", "osm-n1", "osm-n2"])
        self.assertEqual(osm["places"], before["places"])


if __name__ == "__main__":
    unittest.main()
