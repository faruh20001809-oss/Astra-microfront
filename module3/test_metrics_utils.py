from metrics_utils import extract_latency_ms, percentile


def test_extract_latency_ms_prefers_duration_ms_json():
    action = 'request_complete'
    details = '{"duration_ms": 321.5, "status":"ok"} and 200 ms fallback'
    assert extract_latency_ms(action, details) == 321.5


def test_extract_latency_ms_parses_plain_ms():
    assert extract_latency_ms("GET /api", "done in 128 ms") == 128.0


def test_extract_latency_ms_returns_none_when_not_found():
    assert extract_latency_ms("GET /health", "no timing info") is None


def test_percentile_returns_none_on_empty_values():
    assert percentile([], 95) is None


def test_percentile_interpolates_values():
    values = [10, 20, 30, 40]
    assert percentile(values, 50) == 25.0
    assert percentile(values, 95) == 38.5
