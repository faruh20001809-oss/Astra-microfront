from sqlalchemy import create_engine, text

# Подключение к БД
engine = create_engine('postgresql://postgres:root@localhost:5432/museum_user')

with engine.connect() as conn:
    # Сначала преобразуем integer в text, затем в JSONB
    conn.execute(text('ALTER TABLE roles ALTER COLUMN permissions TYPE TEXT'))
    conn.commit()
    print("Тип столбца permissions изменён на TEXT")
    
    conn.execute(text('ALTER TABLE roles ALTER COLUMN permissions TYPE JSONB USING permissions::jsonb'))
    conn.commit()
    print("Тип столбца permissions изменён на JSONB")
    print("Миграция успешно завершена!")
