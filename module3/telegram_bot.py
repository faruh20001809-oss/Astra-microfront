"""
Telegram бот для отправки отчётов
Использование:
1. Получите токен у @BotFather в Telegram
2. Добавьте токен в переменную TELEGRAM_BOT_TOKEN
3. Запустите: python telegram_bot.py
"""

import os
import sys
import asyncio
from datetime import datetime, timedelta
from io import BytesIO

# Токен бота
TELEGRAM_BOT_TOKEN = "8726668588:AAEJHCPicp2kvSjWld3zTsdFR8UDZ9BquCM"

# ID чата для отправки отчётов
TARGET_CHAT_ID = ""

# Подключение к PostgreSQL
DB_URI = 'postgresql://postgres:root@localhost:5432/museum_user?client_encoding=UTF8&connect_timeout=10'

try:
    import telebot
    from telebot.types import InlineKeyboardMarkup, InlineKeyboardButton
except ImportError:
    print("Install library: pip install pyTelegramBotAPI")
    sys.exit(1)

# Подключение SQLAlchemy
try:
    from sqlalchemy import create_engine, func
    from sqlalchemy.orm import sessionmaker
    engine = create_engine(DB_URI, pool_pre_ping=True, pool_recycle=300)
    Session = sessionmaker(bind=engine)
    db_session = Session()
except Exception as e:
    print(f"Database connection error: {e}")
    db_session = None


class MuseumBot:
    def __init__(self, token, db_session=None):
        """Инициализация бота"""
        self.bot = telebot.TeleBot(token, parse_mode='HTML')
        self.db_session = db_session
        self.setup_handlers()
        
    def setup_handlers(self):
        """Настройка обработчиков команд"""
        
        @self.bot.message_handler(commands=['start'])
        def handle_start(message):
            """Обработчик команды /start"""
            welcome_text = """
👋 Добро пожаловать в Museum Analytics Bot!

Я помогу вам получать отчёты о активности пользователей музея.

📊 Доступные команды:
/start - Показать это сообщение
/metrics - Получить текущие метрики
/report - Сгенерировать отчёт за период
/daily - Отчёт за сегодня
/weekly - Отчёт за неделю
/help - Справка

📝 Для настройки отправки отчётов администратору нужно указать TARGET_CHAT_ID.
            """
            self.bot.send_message(message.chat.id, welcome_text)
            
        @self.bot.message_handler(commands=['help'])
        def handle_help(message):
            """Обработчик команды /help"""
            help_text = """
🔍 Справка по боту

<b>Основные команды:</b>
/start - Запуск бота
/metrics - Текущая статистика
/report [дни] - Отчёт за указанное количество дней
/daily - Отчёт за сегодня
/weekly - Отчёт за неделю
/monthly - Отчёт за месяц

<b>Настройка:</b>
Для получения автоматических отчётов администратор должен настроить TARGET_CHAT_ID в коде бота.
            """
            self.bot.send_message(message.chat.id, help_text)
            
        @self.bot.message_handler(commands=['metrics'])
        def handle_metrics(message):
            """Получить текущие метрики"""
            self.bot.send_message(message.chat.id, "📊 Загружаю метрики...")
            
            try:
                # Здесь можно получить метрики из базы данных
                metrics_text = self.get_metrics_text()
                self.bot.send_message(message.chat.id, metrics_text)
            except Exception as e:
                self.bot.send_message(message.chat.id, f"❌ Ошибка: {str(e)}")
                
        @self.bot.message_handler(commands=['daily'])
        def handle_daily(message):
            """Отчёт за сегодня"""
            self.bot.send_message(message.chat.id, "📅 Формирую отчёт за сегодня...")
            self.send_report(message.chat.id, days=1)
            
        @self.bot.message_handler(commands=['weekly'])
        def handle_weekly(message):
            """Отчёт за неделю"""
            self.bot.send_message(message.chat.id, "📅 Формирую отчёт за неделю...")
            self.send_report(message.chat.id, days=7)
            
        @self.bot.message_handler(commands=['monthly'])
        def handle_monthly(message):
            """Отчёт за месяц"""
            self.bot.send_message(message.chat.id, "📅 Формирую отчёт за месяц...")
            self.send_report(message.chat.id, days=30)
            
        @self.bot.message_handler(commands=['report'])
        def handle_report(message):
            """Отчёт за указанный период"""
            try:
                # Парсим аргумент команды
                parts = message.text.split()
                days = int(parts[1]) if len(parts) > 1 else 7
                self.bot.send_message(message.chat.id, f"📊 Формирую отчёт за {days} дней...")
                self.send_report(message.chat.id, days=days)
            except ValueError:
                self.bot.send_message(message.chat.id, "Использование: /report [количество_дней]\nНапример: /report 14")
                
    def get_metrics_text(self):
        """Получить текст метрик из БД"""
        try:
            if not self.db_session:
                return "Database not available"
            
            from sqlalchemy import text
            
            # Всего пользователей
            result = self.db_session.execute(text("SELECT COUNT(*) FROM users"))
            total_users = result.scalar() or 0
            
            # Визитов сегодня
            today = datetime.now().date()
            result = self.db_session.execute(
                text("SELECT COUNT(*) FROM visits WHERE DATE(timestamp) = :today"),
                {"today": today}
            )
            visits_today = result.scalar() or 0
            
            # Всего логов
            result = self.db_session.execute(text("SELECT COUNT(*) FROM logs"))
            total_logs = result.scalar() or 0
            
            # Активные пользователи за последние 30 дней
            thirty_days_ago = datetime.now() - timedelta(days=30)
            result = self.db_session.execute(
                text("SELECT COUNT(DISTINCT user_id) FROM visits WHERE timestamp >= :since AND user_id IS NOT NULL"),
                {"since": thirty_days_ago}
            )
            active_users = result.scalar() or 0
            
            text = f"""
📊 <b>Текущие метрики системы</b>

👥 Пользователи: {total_users}
👁️ Визитов сегодня: {visits_today}
📝 Логов: {total_logs}
📅 Активных пользователей (30 дней): {active_users}

⏰ Обновлено: {datetime.now().strftime('%d.%m.%Y %H:%M')}
            """
            return text
        except Exception as e:
            return f"Ошибка получения метрик: {str(e)}"
        
    def send_report(self, chat_id, days=7):
        """Отправить отчёт"""
        try:
            report_text = self.generate_report(days)
            self.bot.send_message(chat_id, report_text)
        except Exception as e:
            self.bot.send_message(chat_id, f"Ошибка: {str(e)}")
            
    def generate_report(self, days):
        """Сгенерировать отчёт из БД"""
        try:
            if not self.db_session:
                return "База данных недоступна"
            
            from sqlalchemy import text
            
            timestamp = datetime.now().strftime('%d.%m.%Y %H:%M')
            
            # Данные за период
            start_date = datetime.now() - timedelta(days=days)
            
            # Новые пользователи за период
            result = self.db_session.execute(
                text("SELECT COUNT(*) FROM users WHERE created_at >= :start"),
                {"start": start_date}
            )
            new_users = result.scalar() or 0
            
            # Всего визитов за период
            result = self.db_session.execute(
                text("SELECT COUNT(*) FROM visits WHERE timestamp >= :start"),
                {"start": start_date}
            )
            total_visits = result.scalar() or 0
            
            # Логи за период
            result = self.db_session.execute(
                text("SELECT COUNT(*) FROM logs WHERE timestamp >= :start"),
                {"start": start_date}
            )
            total_logs = result.scalar() or 0
            
            # Активные пользователи
            result = self.db_session.execute(
                text("SELECT COUNT(DISTINCT user_id) FROM visits WHERE timestamp >= :start AND user_id IS NOT NULL"),
                {"start": start_date}
            )
            active_users = result.scalar() or 0
            
            # Топ страниц
            result = self.db_session.execute(
                text("""
                    SELECT page, COUNT(*) as cnt 
                    FROM visits 
                    WHERE timestamp >= :start AND page IS NOT NULL
                    GROUP BY page 
                    ORDER BY cnt DESC 
                    LIMIT 5
                """),
                {"start": start_date}
            )
            top_pages = result.fetchall()
            
            # Формируем текст отчёта
            text = f"""
📊 <b>Отчёт за {days} дней</b>

📅 Период: {days} дней
👥 Новых пользователей: {new_users}
👁️ Всего визитов: {total_visits}
📝 Логов: {total_logs}
👤 Активных пользователей: {active_users}
            """
            
            if top_pages:
                text += "\n<b>Топ страниц:</b>\n"
                for page, cnt in top_pages:
                    page_name = (page[:40] + '...') if page and len(page) > 40 else (page or 'Неизвестно')
                    text += f"• {page_name}: {cnt}\n"
            
            text += f"\n⏰ Сформировано: {timestamp}"
            return text
            
        except Exception as e:
            return f"Ошибка генерации отчёта: {str(e)}"
        
    def send_pdf_report(self, chat_id, pdf_bytes, filename="report.pdf"):
        """Отправить PDF отчёт"""
        try:
            self.bot.send_document(
                chat_id, 
                pdf_bytes,
                caption="📄 Ваш отчёт готов!",
                visible_file_name=filename
            )
        except Exception as e:
            self.bot.send_message(chat_id, f"❌ Ошибка отправки: {str(e)}")
            
    async def send_scheduled_report(self, days=7):
        """Отправить запланированный отчёт (для cron задач)"""
        if not TARGET_CHAT_ID:
            print("Please set TARGET_CHAT_ID for auto-sending")
            return
            
        report_text = self.generate_report(days)
        self.bot.send_message(TARGET_CHAT_ID, report_text)
        
    def run(self):
        """Запуск бота"""
        print("Museum Bot started...")
        print("Press Ctrl+C to stop")
        try:
            self.bot.infinity_polling(timeout=60, long_polling_timeout=60)
        except KeyboardInterrupt:
            print("\nBot stopped")
            
    def run_once_and_exit(self):
        """Однократная отправка отчёта (для демонстрации)"""
        if not TARGET_CHAT_ID:
            print("Please set TARGET_CHAT_ID")
            print("   Get chat ID via @userinfobot in Telegram")
            return
            
        print(f"📊 Отправляю отчёт в чат {TARGET_CHAT_ID}...")
        self.send_report(TARGET_CHAT_ID, days=7)
        print("✅ Отчёт отправлен!")


def main():
    """Точка входа"""
    if not TELEGRAM_BOT_TOKEN or TELEGRAM_BOT_TOKEN == "YOUR_BOT_TOKEN_HERE":
        print("Пожалуйста, добавьте токен бота в переменную TELEGRAM_BOT_TOKEN")
        return
        
    # Создаём бота с подключением к БД
    bot = MuseumBot(TELEGRAM_BOT_TOKEN, db_session)
    print("Museum Bot запущен с подключением к БД...")
    print("Нажмите Ctrl+C для остановки")
    bot.run()


if __name__ == "__main__":
    main()
