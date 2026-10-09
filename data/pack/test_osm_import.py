import unittest

from osm_import import (
    boundary_edges,
    mostly_inside,
    point_in_boundary,
    build_places,
    build_segments,
    chain_ways,
    cumulative_m,
    cut_line,
    haversine_m,
    minutes_est,
    point_at,
    project_on_line,
    route_name,
    signboards,
)
from shapes import decode_polyline, encode_polyline

# 0.001 deg lat is ~111 m; 0.001 deg lng at lat 14.55 is ~107.6 m.
LAT0, LNG0 = 14.55, 121.02


def north(p, deg):
    return (p[0] + deg, p[1])


def east(p, deg):
    return (p[0], p[1] + deg)


class PolylineTest(unittest.TestCase):
    def test_round_trip(self):
        points = [(14.558012, 121.018547), (14.561313, 121.014922), (14.55, 121.0)]
        self.assertEqual(decode_polyline(encode_polyline(points, 6), 6), points)


class ChainTest(unittest.TestCase):
    def test_flipped_way(self):
        a = [(LAT0, LNG0), (LAT0 + 0.001, LNG0)]
        # second way is stored end-to-start
        b = [(LAT0 + 0.002, LNG0), (LAT0 + 0.001, LNG0)]
        line, reason = chain_ways([a, b])
        self.assertIsNone(reason)
        self.assertEqual(line, [(LAT0, LNG0), (LAT0 + 0.001, LNG0), (LAT0 + 0.002, LNG0)])

    def test_flipped_first_way(self):
        a = [(LAT0 + 0.001, LNG0), (LAT0, LNG0)]
        b = [(LAT0 + 0.001, LNG0), (LAT0 + 0.002, LNG0)]
        line, reason = chain_ways([a, b])
        self.assertIsNone(reason)
        self.assertEqual(line[0], (LAT0, LNG0))
        self.assertEqual(line[-1], (LAT0 + 0.002, LNG0))

    def test_small_gap_is_joined(self):
        a = [(LAT0, LNG0), (LAT0 + 0.001, LNG0)]
        b = [(LAT0 + 0.001 + 0.0002, LNG0), (LAT0 + 0.002, LNG0)]  # ~22 m gap
        line, reason = chain_ways([a, b])
        self.assertIsNone(reason)
        self.assertEqual(len(line), 4)

    def test_gap_detection(self):
        a = [(LAT0, LNG0), (LAT0 + 0.001, LNG0)]
        b = [(LAT0 + 0.001 + 0.001, LNG0), (LAT0 + 0.003, LNG0)]  # ~111 m gap
        line, reason = chain_ways([a, b])
        self.assertIsNone(line)
        self.assertTrue(reason.startswith("gap"))

    def test_no_ways(self):
        self.assertEqual(chain_ways([]), (None, "no ways"))


class ProjectionTest(unittest.TestCase):
    def setUp(self):
        # L shape: 0.004 deg north (~443 m) then 0.004 deg east (~430 m)
        corner = (LAT0 + 0.004, LNG0)
        self.line = [(LAT0, LNG0), corner, (corner[0], LNG0 + 0.004)]
        self.cum = cumulative_m(self.line)

    def test_cumulative(self):
        self.assertAlmostEqual(self.cum[1], haversine_m(self.line[0], self.line[1]), places=3)
        self.assertGreater(self.cum[2], self.cum[1])

    def test_projection_with_offset(self):
        # 20 m-ish west of the first leg, halfway up
        stop = (LAT0 + 0.002, LNG0 - 0.0002)
        along, off = project_on_line(self.line, self.cum, stop)
        self.assertAlmostEqual(along, self.cum[1] / 2, delta=1)
        self.assertAlmostEqual(off, 21.5, delta=1)

    def test_point_at_and_cut_across_corner(self):
        a, b = self.cum[1] - 100, self.cum[1] + 100
        points = cut_line(self.line, self.cum, a, b)
        self.assertEqual(len(points), 3)
        self.assertEqual(points[1], self.line[1])
        length = sum(haversine_m(p, q) for p, q in zip(points, points[1:]))
        self.assertAlmostEqual(length, 200, delta=0.5)
        self.assertEqual(point_at(self.line, self.cum, 0), self.line[0])
        self.assertEqual(point_at(self.line, self.cum, self.cum[-1] + 5), self.line[-1])

    def test_build_segments_two_stops(self):
        s1 = (1, "First", LAT0 + 0.001, LNG0 - 0.0001)
        s2 = (2, "Second", self.line[1][0] + 0.0001, LNG0 + 0.003)
        kept, segs, drops = build_segments(self.line, [s1, s2])
        self.assertEqual([k[0] for k in kept], [1, 2])
        self.assertEqual(len(segs), 1)
        _, _, points, distance = segs[0]
        expected = (self.cum[1] - 0.001 * 111_195 + 0.003 * 107_600)
        self.assertAlmostEqual(distance, expected, delta=15)
        self.assertEqual(len(points), 3)  # start projection, corner, end projection

    def test_far_stop_dropped_and_non_increasing_dropped(self):
        near = (1, "A", LAT0 + 0.001, LNG0)
        far = (2, "Far", LAT0 + 0.002, LNG0 + 0.002)  # ~215 m from line
        back = (3, "Back", LAT0 + 0.0005, LNG0)  # before A
        after = (4, "After", LAT0 + 0.003, LNG0)
        kept, segs, drops = build_segments(self.line, [near, far, back, after])
        self.assertEqual([k[0] for k in kept], [1, 4])
        self.assertEqual(drops["stop off line"], 1)
        self.assertEqual(drops["stop not increasing"], 1)

    def test_segment_length_filter(self):
        a = (1, "A", LAT0 + 0.0010, LNG0)
        b = (2, "B", LAT0 + 0.0013, LNG0)  # ~33 m: too short
        c = (3, "C", LAT0 + 0.0040, LNG0)
        kept, segs, drops = build_segments(self.line, [a, b, c])
        self.assertEqual(drops["segment too short"], 1)
        self.assertEqual([(s[0][0], s[1][0]) for s in segs], [(2, 3)])
        kept, segs, drops = build_segments(self.line, [a, c], seg_max=200)
        self.assertEqual(drops["segment too long"], 1)
        self.assertEqual(segs, [])


class BoundaryTest(unittest.TestCase):
    def test_even_odd_with_hole(self):
        outer = [(0, 0), (0, 10), (10, 10), (10, 0), (0, 0)]
        hole = [(4, 4), (4, 6), (6, 6), (6, 4), (4, 4)]
        edges = boundary_edges([outer, hole])
        self.assertTrue(point_in_boundary((2, 2), edges))
        self.assertFalse(point_in_boundary((5, 5), edges))
        self.assertFalse(point_in_boundary((12, 5), edges))

    def test_barangay_ref_decides_before_outline(self):
        edges = boundary_edges([[(0, 0), (0, 10), (10, 10), (10, 0), (0, 0)]])
        geom = [{"lat": 2, "lon": 2}, {"lat": 3, "lon": 3}]
        rel = {"type": "relation", "members": [{"type": "way", "geometry": geom}], "tags": {}}
        self.assertTrue(mostly_inside(rel, edges))
        rel["tags"] = {"ref": "1381500035"}
        self.assertFalse(mostly_inside(rel, edges))
        rel["tags"] = {"ref": "1380300001"}
        self.assertTrue(mostly_inside(rel, edges))


class MinutesTest(unittest.TestCase):
    def test_rounding_up(self):
        self.assertEqual(minutes_est(848), 8)  # 7.07 rounds up
        self.assertEqual(minutes_est(840), 7)
        self.assertEqual(minutes_est(841), 8)
        self.assertEqual(minutes_est(50), 1)
        self.assertEqual(minutes_est(6000), 50)


class MetaTest(unittest.TestCase):
    def test_signboards_dedupe(self):
        self.assertEqual(signboards({"ref": "X", "to": "Y", "name": "X"}), ["X", "Y"])
        self.assertEqual(signboards({"ref": " ", "name": "Z"}), ["Z"])

    def test_route_name_fallback(self):
        self.assertEqual(route_name({"name": "N"}), "N")
        self.assertEqual(route_name({"ref": "7", "from": "A", "to": "B"}), "7 A – B")


class PlacesTest(unittest.TestCase):
    def node(self, i, lat, lng, **tags):
        return {"type": "node", "id": i, "lat": lat, "lon": lng, "tags": tags}

    def test_places(self):
        elements = [
            self.node(1, 14.55, 121.02, name="Park A", leisure="park"),
            self.node(2, 14.55, 121.02, name="12345", amenity="school"),
            self.node(3, 14.60, 121.02, name="Outside Mall", shop="mall"),
            {
                "type": "way",
                "id": 4,
                "center": {"lat": 14.551, "lon": 121.021},
                "tags": {
                    "name": "Big Mall",
                    "shop": "mall",
                    "alt_name": "BM;Big M",
                    "name:en": "Big Mall PH",
                },
            },
            self.node(5, 14.551, 121.021, name="Big Mall", shop="mall"),  # store node: dropped, only way/relation malls count
            {
                "type": "relation",
                "id": 6,
                "center": {"lat": 14.56, "lon": 121.01},
                "tags": {"name": "Poblacion", "boundary": "administrative", "admin_level": "10"},
            },
        ]
        places, total = build_places(elements)
        self.assertEqual(total, 3)
        self.assertEqual([p["id"] for p in places], ["osm-r6", "osm-w4", "osm-n1"])
        self.assertEqual([p["kind"] for p in places], ["barangay", "mall", "park"])
        self.assertEqual(places[1]["aliases"], ["Big Mall PH", "BM", "Big M"])

    def test_same_name_and_kind_nearby_is_deduped(self):
        elements = [
            self.node(1, 14.55, 121.02, name="Park A", leisure="park"),
            {"type": "way", "id": 2, "center": {"lat": 14.5501, "lon": 121.0201},
             "tags": {"name": "Park A", "leisure": "park", "alt_name": "PA"}},
        ]
        places, total = build_places(elements)
        self.assertEqual([p["id"] for p in places], ["osm-w2"])

    def test_cap_prefers_barangays(self):
        elements = [self.node(i, 14.55, 121.02 + i * 0.001, name=f"School {i}", amenity="school") for i in range(1, 6)]
        elements.append(self.node(99, 14.56, 121.02, name="Brgy", boundary="administrative", admin_level="10"))
        places, total = build_places(elements, cap=2)
        self.assertEqual(total, 6)
        self.assertEqual(places[0]["kind"], "barangay")
        self.assertEqual(len(places), 2)


if __name__ == "__main__":
    unittest.main()
