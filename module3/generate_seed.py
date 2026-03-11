import random
from datetime import datetime, timedelta

NUM_USERS = 100
NUM_VISITS = 500
NUM_LOGS = 200

def generate_users_sql():
    users_sql = []
    for i in range(1, NUM_USERS + 1):
        name = f'Пользователь {i}'
        email = f'user{i}@example.com'
        password = '123456'  # Простой пароль
        role_id = 1
        created_at = (datetime.now() - timedelta(days=random.randint(1, 90))).strftime('%Y-%m-%d %H:%M:%S')
        last_login = (datetime.now() - timedelta(days=random.randint(0, 10))).strftime('%Y-%m-%d %H:%M:%S')
        users_sql.append(
            f"INSERT INTO users (name, email, password, role_id, created_at, last_login) VALUES "
            f"('{name}', '{email}', '{password}', {role_id}, '{created_at}', '{last_login}');"
        )
    return users_sql

def generate_visits_sql():
    visits_sql = []
    for _ in range(NUM_VISITS):
        user_id = random.randint(1, NUM_USERS)
        page = random.choice(['/','/gallery','/exhibit/1','/exhibit/2','/about'])
        timestamp = (datetime.now() - timedelta(minutes=random.randint(1, 10000))).strftime('%Y-%m-%d %H:%M:%S')
        visits_sql.append(
            f"INSERT INTO visits (user_id, page, timestamp) VALUES ({user_id}, '{page}', '{timestamp}');"
        )
    return visits_sql

def generate_logs_sql():
    logs_sql = []
    actions = ['Добавлен экспонат', 'Удалён экспонат', 'Комментарий', 'Обновление профиля']
    for _ in range(NUM_LOGS):
        user_id = random.randint(1, NUM_USERS)
        action = random.choice(actions)
        details = f'{action} №{random.randint(1, 100)}'
        timestamp = (datetime.now() - timedelta(minutes=random.randint(1, 10000))).strftime('%Y-%m-%d %H:%M:%S')
        logs_sql.append(
            f"INSERT INTO logs (user_id, action, details, timestamp) VALUES ({user_id}, '{action}', '{details}', '{timestamp}');"
        )
    return logs_sql

# Собрать весь скрипт
sql_statements = generate_users_sql() + generate_visits_sql() + generate_logs_sql()

# Записать в файл
with open("seed_data.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_statements))

print("SQL-скрипт записан в файл seed_data.sql")
