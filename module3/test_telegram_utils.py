from datetime import datetime

from telegram_utils import order_message, status_label


def test_status_label_maps_known_values():
    assert status_label("processing") == "В обработке"
    assert status_label("DELIVERED") == "Доставлен"


def test_order_message_contains_main_fields():
    msg = order_message(
        order_id="ORD-123",
        status="CONFIRMED",
        tracking_number="TRK-55",
        total=1490,
        currency="RUB",
        now=datetime(2026, 3, 31, 12, 30),
    )
    assert "Заказ ORD-123" in msg
    assert "Подтвержден" in msg
    assert "TRK-55" in msg
    assert "1490 RUB" in msg
