#!/bin/bash
# Создание venv и установка зависимостей для module3 (Debian 12: pip не ставит пакеты в систему).
# Запуск: bash setup_venv.sh
# На сервере может понадобиться: apt-get install -y python3-venv

set -e
cd "$(dirname "$0")"

if [ ! -d "venv" ]; then
  echo "Создаём виртуальное окружение venv..."
  python3 -m venv venv || { echo "Установите python3-venv: apt-get install -y python3-venv"; exit 1; }
fi
echo "Устанавливаем зависимости в venv..."
venv/bin/pip install --upgrade pip
venv/bin/pip install -r requirements.txt
echo "Готово. Запуск: venv/bin/python app.py  или  venv/bin/gunicorn -b 127.0.0.1:5000 app:app"
echo "Или активируйте venv: source venv/bin/activate && python app.py"
