import json
import os
import tempfile
import unittest

from shapes import (
    fill_hero,
    trim_overshoot,
    ShapeError,
    check_segment,
    length_m,
    encode_polyline,
    decode_polyline,
)

START = (14.561313, 121.014922)
END = (14.557850, 121.007780)


class ShapeTest(unittest.TestCase):
    def test_polyline6_roundtrip_and_length(self):
        points = [(14.561313, 121.014922), (14.560000, 121.012000), (14.557850, 121.007780)]
        encoded = encode_polyline(points, 6)
        back = decode_polyline(encoded, 6)
        for got, want in zip(back, points):
            assert abs(got[0] - want[0]) < 1e-6
            assert abs(got[1] - want[1]) < 1e-6
        assert length_m(points) == length_m(back)
        assert length_m([points[0], points[0]]) == 0

    def test_trim_cuts_the_loop_past_the_stop(self):
        near = (14.55790, 121.00780)   # within 30 m of END
        far = (14.55750, 121.00684)    # the live overshoot, about 109 m past END
        trimmed = trim_overshoot([START, near, far, END], END)
        assert far not in trimmed
        assert trimmed[-1] == near

    def test_trim_keeps_a_line_that_stops(self):
        line = [START, (14.559000, 121.011000), END]
        assert trim_overshoot(line, END) == line

    def test_check_names_a_missing_or_bad_line(self):
        with self.assertRaises(ShapeError) as missing:
            check_segment("jeep-buendia-lrt", [], START, END, 0)
        assert "jeep-buendia-lrt" in str(missing.exception)
        straight = [START, END]
        with self.assertRaises(ShapeError):
            check_segment("bus-buendia-lrt", straight, START, END, length_m(straight))
        far_end = (14.50, 121.00)
        bent = [START, (14.560000, 121.012000), far_end]
        with self.assertRaises(ShapeError):
            check_segment("jeep-buendia-lrt", bent, START, END, length_m(bent))
        good = [START, (14.560000, 121.012000), (14.558500, 121.009000), END]
        with self.assertRaises(ShapeError):
            check_segment("jeep-buendia-lrt", good, START, END, length_m(good) + 5)
        check_segment("jeep-buendia-lrt", good, START, END, length_m(good))

    def test_fill_uses_the_cache_and_keeps_fares(self):
        # source is a copy of the two hero segments with shape and distance_m null
        # cache holds an OSRM body whose geometry is encode_polyline of a line that
        # reaches within 30 m of the get-off stop, then includes (14.55750, 121.00684), then the stop
        near = (14.55790, 121.00780)  # within 30 m of END
        far = (14.55750, 121.00684)
        fixture = [
            START,
            (14.560000, 121.012000),
            (14.558500, 121.009000),
            near,
            far,
            END,
        ]
        pack_dir = os.path.dirname(os.path.abspath(__file__))
        with open(os.path.join(pack_dir, "hero-trip.source.json"), encoding="utf-8") as hero:
            source_data = json.load(hero)
        for segment in source_data["segments"]:
            segment["shape"] = None
            segment["distance_m"] = None
        with tempfile.TemporaryDirectory() as tmp:
            source = os.path.join(tmp, "hero-trip.source.json")
            cache = os.path.join(tmp, "osrm-gil-puyat-driving.json")
            with open(source, "w", encoding="utf-8") as out:
                json.dump(source_data, out)
            with open(cache, "w", encoding="utf-8") as out:
                json.dump({"code": "Ok", "routes": [{"geometry": encode_polyline(fixture, 6)}]}, out)
            calls = []
            def fetch():
                calls.append(1)
                raise AssertionError("network")
            fill_hero(source, cache, fetch)
            assert calls == []
            with open(source, encoding="utf-8") as filled:
                rides = [s for s in json.load(filled)["segments"]]
            assert rides[0]["shape"]["polyline"] == rides[1]["shape"]["polyline"]
            assert rides[0]["shape"]["engine"] == "osrm"
            assert rides[0]["shape"]["profile"] == "driving"
            assert rides[0]["fare_php"] == 14 and rides[1]["fare_php"] == 15
            assert rides[0]["minutes"] == 7 and rides[1]["minutes"] == 7
            points = decode_polyline(rides[0]["shape"]["polyline"], 6)
            assert (14.55750, 121.00684) not in [(round(p[0], 5), round(p[1], 5)) for p in points]
            check_segment(rides[0]["route_id"], points, START, END, rides[0]["distance_m"])

    def test_fill_keeps_generated_at_and_one_osrm_credit(self):
        near = (14.55790, 121.00780)
        far = (14.55750, 121.00684)
        fixture = [
            START,
            (14.560000, 121.012000),
            (14.558500, 121.009000),
            near,
            far,
            END,
        ]
        pack_dir = os.path.dirname(os.path.abspath(__file__))
        with open(os.path.join(pack_dir, "hero-trip.source.json"), encoding="utf-8") as hero:
            source_data = json.load(hero)
        for segment in source_data["segments"]:
            segment["shape"] = None
            segment["distance_m"] = None
        with tempfile.TemporaryDirectory() as tmp:
            source = os.path.join(tmp, "hero-trip.source.json")
            cache = os.path.join(tmp, "osrm-gil-puyat-driving.json")
            with open(source, "w", encoding="utf-8") as out:
                json.dump(source_data, out)
            with open(cache, "w", encoding="utf-8") as out:
                json.dump({"code": "Ok", "routes": [{"geometry": encode_polyline(fixture, 6)}]}, out)

            def fetch():
                raise AssertionError("network")

            fill_hero(source, cache, fetch)
            with open(source, encoding="utf-8") as filled:
                first = json.load(filled)
            generated_at = first["segments"][0]["shape"]["generated_at"]
            later = os.path.getmtime(cache) + 10_000
            os.utime(cache, (later, later))
            fill_hero(source, cache, fetch)
            with open(source, encoding="utf-8") as filled:
                second = json.load(filled)
            osrm = [
                line
                for line in second["meta"]["sources"]
                if line.startswith("OSRM demo server, driving profile, polyline6")
            ]
            assert len(osrm) == 1
            assert second["segments"][0]["shape"]["generated_at"] == generated_at
            assert second["segments"][1]["shape"]["generated_at"] == generated_at
            assert second["meta"]["notes"] == (
                "Source file for the hero trip only (state.md D37). "
                "Road shapes and distance_m come from the cached OSRM driving line."
            )
