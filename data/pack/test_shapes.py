import unittest

from shapes import encode_polyline, decode_polyline, length_m


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
