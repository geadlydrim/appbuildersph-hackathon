import math

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
