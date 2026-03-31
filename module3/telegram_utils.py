from datetime import datetime


def status_label(status_raw: str) -> str:
    status = (status_raw or "").strip().upper()
    return {
        "PROCESSING": "В обработке",
        "CONFIRMED": "Подтвержден",
        "SHIPPED": "Отправлен",
        "DELIVERED": "Доставлен",
        "CANCELLED": "Отменен",
    }.get(status, status_raw or "—")


def order_message(order_id: str, status: str, tracking_number: str, total, currency: str, now=None) -> str:
    now = now or datetime.now()
    lines = [
        f"📦 <b>Заказ {order_id or '—'}</b>",
        f"Статус: <b>{status_label(status)}</b>",
    ]
    if tracking_number:
        lines.append(f"Трек-номер: <code>{tracking_number}</code>")
    if total is not None:
        lines.append(f"Сумма: {total} {currency or 'RUB'}")
    lines.append(f"Время: {now.strftime('%d.%m.%Y %H:%M')}")
    return "\n".join(lines)
