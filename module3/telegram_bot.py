"""
Telegram bot for account linking and order status notifications.

Features:
- /start and /start link_<token> deep-link flow
- Internal HTTP endpoint for Java backend:
  POST /internal/telegram/send-order-status

Required environment variables:
- TELEGRAM_BOT_TOKEN
- TELEGRAM_INTERNAL_TOKEN

Optional:
- TELEGRAM_JAVA_API_BASE (default http://localhost:8080/api/v1)
- TELEGRAM_INTERNAL_HOST (default 127.0.0.1)
- TELEGRAM_INTERNAL_PORT (default 8090)
"""

import os
import threading

import requests
import telebot
from flask import Flask, jsonify, request
from telegram_utils import order_message


BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
JAVA_API_BASE = os.getenv("TELEGRAM_JAVA_API_BASE", "http://localhost:8080/api/v1").rstrip("/")
SERVICE_TOKEN = os.getenv("TELEGRAM_INTERNAL_TOKEN", "").strip()
INTERNAL_HOST = os.getenv("TELEGRAM_INTERNAL_HOST", "127.0.0.1")
INTERNAL_PORT = int(os.getenv("TELEGRAM_INTERNAL_PORT", "8090"))

if not BOT_TOKEN:
    raise RuntimeError("TELEGRAM_BOT_TOKEN is required.")
if not SERVICE_TOKEN:
    raise RuntimeError("TELEGRAM_INTERNAL_TOKEN is required.")

bot = telebot.TeleBot(BOT_TOKEN, parse_mode="HTML")
app = Flask(__name__)


def _order_message(order_id: str, status: str, tracking_number: str, total, currency: str) -> str:
    return order_message(order_id, status, tracking_number, total, currency)


def confirm_link_token(token: str, chat_id: int, username: str) -> tuple[bool, str]:
    try:
        response = requests.post(
            f"{JAVA_API_BASE}/internal/telegram/link/confirm",
            headers={
                "Content-Type": "application/json",
                "Accept": "application/json",
                "X-Service-Token": SERVICE_TOKEN,
            },
            json={
                "token": token,
                "chatId": chat_id,
                "username": username or "",
            },
            timeout=10,
        )
        if response.status_code >= 400:
            payload = response.json() if response.headers.get("content-type", "").startswith("application/json") else {}
            return False, payload.get("message") or "Не удалось подтвердить привязку."
        return True, "Telegram успешно привязан. Теперь статусы заказов будут приходить сюда."
    except Exception:
        return False, "Сервис привязки временно недоступен. Попробуйте позже."


@bot.message_handler(commands=["start"])
def handle_start(message):
    text = (message.text or "").strip()
    parts = text.split(maxsplit=1)
    payload = parts[1] if len(parts) > 1 else ""
    if payload.startswith("link_"):
        token = payload[len("link_") :].strip()
        ok, msg = confirm_link_token(token, message.chat.id, message.from_user.username or "")
        bot.send_message(message.chat.id, ("✅ " if ok else "❌ ") + msg)
        return

    bot.send_message(
        message.chat.id,
        (
            "Привет! Этот бот используется для привязки Telegram к заказам и получения статусов.\n\n"
            "Для привязки откройте deep-link из сайта."
        ),
    )


@bot.message_handler(commands=["unlink"])
def handle_unlink(message):
    bot.send_message(
        message.chat.id,
        "Отвязка через бота пока не реализована. Используйте настройку в веб-приложении.",
    )


@app.post("/internal/telegram/send-order-status")
def send_order_status():
    token = request.headers.get("X-Service-Token", "")
    if token != SERVICE_TOKEN:
        return jsonify({"status": "error", "message": "Unauthorized"}), 401

    body = request.get_json(silent=True) or {}
    chat_id = body.get("chatId")
    if chat_id is None:
        return jsonify({"status": "error", "message": "chatId is required"}), 400

    msg = _order_message(
        str(body.get("orderId") or ""),
        str(body.get("status") or ""),
        str(body.get("trackingNumber") or ""),
        body.get("total"),
        str(body.get("currency") or "RUB"),
    )
    try:
        bot.send_message(int(chat_id), msg)
        return jsonify({"status": "ok"})
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 502


def run_http_server():
    app.run(host=INTERNAL_HOST, port=INTERNAL_PORT, debug=False, use_reloader=False)


def main():
    threading.Thread(target=run_http_server, daemon=True).start()
    bot.infinity_polling(timeout=60, long_polling_timeout=60)


if __name__ == "__main__":
    main()
