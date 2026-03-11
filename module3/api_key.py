import json
import subprocess

# Загружаем ключ сервисного аккаунта
with open("key.json") as f:
    key_data = json.load(f)

# Генерация IAM-токена через yc CLI
result = subprocess.run(
    ["yc", "iam", "create-token", "--service-account-key-file", "key.json"],
    capture_output=True,
    text=True
)
iam_token = result.stdout.strip()
print("Ваш новый IAM-токен:", iam_token)
