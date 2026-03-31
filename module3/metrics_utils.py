import re

LATENCY_RE = re.compile(r'(\d+(?:\.\d+)?)\s*ms', re.IGNORECASE)
LATENCY_JSON_RE = re.compile(r'"duration_ms"\s*:\s*(\d+(?:\.\d+)?)', re.IGNORECASE)


def extract_latency_ms(action: str, details: str):
    text_blob = f"{action or ''} {details or ''}"
    match = LATENCY_JSON_RE.search(text_blob)
    if match:
        try:
            return float(match.group(1))
        except Exception:
            pass
    match = LATENCY_RE.search(text_blob)
    if match:
        try:
            return float(match.group(1))
        except Exception:
            return None
    return None


def percentile(values, p):
    if not values:
        return None
    vals = sorted(float(v) for v in values)
    if len(vals) == 1:
        return round(vals[0], 2)
    k = (len(vals) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(vals) - 1)
    if f == c:
        return round(vals[f], 2)
    d0 = vals[f] * (c - k)
    d1 = vals[c] * (k - f)
    return round(d0 + d1, 2)
