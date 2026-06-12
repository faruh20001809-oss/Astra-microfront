# Подключение к PostgreSQL с параметрами стабильности
import os
import structlog
from flask import Flask, jsonify, request, render_template, session, redirect, g, abort, url_for
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from flask_login import LoginManager, UserMixin, login_user, login_required, logout_user, current_user
from flask_cors import CORS
from werkzeug.security import check_password_hash as werkzeug_check_password
import bcrypt
from datetime import datetime, date, timedelta
from functools import wraps
from sqlalchemy import func, text
from appl.extensions import db
from appl.models import Role, User, Visit, Log
from appl.schemas import ma, user_schema, users_schema, login_schema, user_update_schema, role_schema, roles_schema, role_update_schema
from metrics_utils import extract_latency_ms, percentile
from flask import abort


def _compute_ops_snapshot(hours_window=24):
    rows = Log.query.filter(
        Log.timestamp >= datetime.utcnow() - timedelta(hours=hours_window)
    ).all()
    actions = [str(r.action or '').lower() for r in rows]
    total = len(actions) or 1
    e4xx = sum(1 for a in actions if '4xx' in a or 'warn' in a)
    e5xx = sum(1 for a in actions if '5xx' in a or 'error' in a or 'exception' in a)
    error_rate = round(((e4xx + e5xx) / total) * 100, 2)

    latencies = []
    for r in rows:
        ms = extract_latency_ms(r.action, r.details)
        if ms is not None:
            latencies.append(ms)
    p50 = percentile(latencies, 50) if latencies else None
    p95 = percentile(latencies, 95) if latencies else None
    p99 = percentile(latencies, 99) if latencies else None
    return {
        "rows": rows,
        "error_rate": error_rate,
        "errors_4xx": e4xx,
        "errors_5xx": e5xx,
        "latency": {"p50": p50, "p95": p95, "p99": p99},
        "latency_samples": len(latencies),
    }

def _get_telegram_link_by_email(email: str):
    if not email:
        return None
    try:
        row = db.session.execute(
            text("""
                SELECT telegram_chat_id, telegram_username, is_verified, linked_at
                FROM user_telegram_links
                WHERE lower(email) = lower(:email) AND is_verified = true
                ORDER BY linked_at DESC
                LIMIT 1
            """),
            {"email": email},
        ).mappings().first()
        return dict(row) if row else None
    except Exception:
        # Если таблица еще не создана миграциями Java, считаем, что привязки нет.
        return None


class PrefixMiddleware:
    """Устанавливает SCRIPT_NAME из X-Forwarded-Prefix (при работе за nginx по подпути /dashboard/)."""
    def __init__(self, app):
        self.app = app

    def __call__(self, environ, start_response):
        prefix = (environ.get('HTTP_X_FORWARDED_PREFIX') or os.environ.get('APPLICATION_ROOT') or '').strip().rstrip('/')
        if prefix:
            environ['SCRIPT_NAME'] = prefix
        return self.app(environ, start_response)


app = Flask(__name__)

# Поддержка подпути при проксировании (например /dashboard/)
app.wsgi_app = PrefixMiddleware(app.wsgi_app)

# Настройка структурированного логирования structlog
import logging
import sys

# Каталог для логов (создаётся при первом запуске)
os.makedirs("logs", exist_ok=True)

# Настройка стандартного логгера для записи в файл
logging.basicConfig(
    level=logging.INFO,
    format="%(message)s",
    handlers=[
        logging.FileHandler("logs/app.log", encoding="utf-8"),
        logging.StreamHandler(sys.stdout)
    ]
)

structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="%Y-%m-%d %H:%M:%S"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.dev.ConsoleRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

log = structlog.get_logger()


def _hash_password(password: str) -> str:
    """Хеш пароля в BCrypt для совместимости с Java (workflow login)."""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')


def _check_password(password: str, password_hash: str) -> bool:
    """Проверка пароля: сначала BCrypt (как в Java), затем legacy Werkzeug."""
    if not password_hash:
        return False
    if password_hash.startswith('$2') or password_hash.startswith('$2a') or password_hash.startswith('$2b'):
        try:
            return bcrypt.checkpw(password.encode('utf-8'), password_hash.encode('utf-8'))
        except Exception:
            return False
    return werkzeug_check_password(password_hash, password)


# Подключение к БД (та же, что и Java: задайте DATABASE_URL в prod)
db_uri = os.environ.get(
    'DATABASE_URL',
    'postgresql://postgres:root@localhost:5432/museum_user?client_encoding=UTF8&connect_timeout=10'
)
# Heroku и др. передают postgres:// — SQLAlchemy требует postgresql://
if db_uri.startswith('postgres://'):
    db_uri = db_uri.replace('postgres://', 'postgresql://', 1)
app.config['SQLALCHEMY_DATABASE_URI'] = db_uri
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {
    'pool_pre_ping': True,
    'pool_recycle': 300,
}
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'CHANGE_THIS_SECRET')

db.init_app(app)
ma.init_app(app)
migrate = Migrate(app, db)

# Автоматическая миграция типа столбца permissions
with app.app_context():
    try:
        # Удаляем значение по умолчанию
        db.session.execute(db.text('ALTER TABLE roles ALTER COLUMN permissions DROP DEFAULT'))
        db.session.commit()
        # Сначала преобразуем integer в text
        db.session.execute(db.text('ALTER TABLE roles ALTER COLUMN permissions TYPE TEXT'))
        db.session.commit()
        # Затем преобразуем text в JSONB
        db.session.execute(db.text('ALTER TABLE roles ALTER COLUMN permissions TYPE JSONB USING permissions::jsonb'))
        db.session.commit()
        # Устанавливаем новое значение по умолчанию
        db.session.execute(db.text("ALTER TABLE roles ALTER COLUMN permissions SET DEFAULT '{}'::jsonb"))
        db.session.commit()
        log.info('Тип столбца permissions успешно изменён на JSONB')
    except Exception as e:
        log.info('Тип столбца permissions уже исправлен или таблица не существует', error=str(e))
    try:
        db.session.execute(db.text('ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE NOT NULL'))
        db.session.commit()
        db.session.execute(db.text('UPDATE users SET is_active = TRUE WHERE is_active IS NULL'))
        db.session.commit()
        db.session.execute(db.text('CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active)'))
        db.session.commit()
        log.info('Столбец is_active добавлен в таблицу users')
    except Exception as e:
        log.info('Столбец is_active уже существует или таблица не существует', error=str(e))

# Инициализация Flask-Login
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login_page'

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))


@app.context_processor
def inject_script_root():
    """Для шаблонов: префикс подпути (например /dashboard) при работе за nginx."""
    return {'root': request.script_root or ''}


# ---------------- AUTH / ACCESS CONTROL ----------------
@app.before_request
def before_request():
    g.user = current_user
    
    # ✅ ЗАЩИТА: редирект неавторизованных на логин
    protected_paths = ['/users', '/roles']
    if any(request.path.startswith(p) for p in protected_paths) and not current_user.is_authenticated:
        return redirect(url_for('login_page'))
    
    if not request.path.startswith("/api") and not request.path.startswith("/static"):
        visit = Visit(user_id=current_user.id if current_user.is_authenticated else None, page=request.path)
        db.session.add(visit)
        db.session.commit()

def role_required(*roles):
    def wrapper(f):
        @wraps(f)
        def decorated(*args, **kwargs):
            if not g.user:
                return redirect(url_for('login_page'))  # ✅ HTML редирект
            if not g.user.role or g.user.role.name not in roles:
                return abort(403)  # ✅ HTML 403
            return f(*args, **kwargs)
        return decorated
    return wrapper


def _user_type_by_role(role_name: str) -> str:
    name = (role_name or "").strip().lower()
    if name in {"администратор", "administrator", "admin"}:
        return "admin"
    if name in {"сотрудник", "модератор", "редактор", "employee", "staff", "moderator", "editor"}:
        return "staff"
    if name in {"клиент", "customer", "client", "guest"}:
        return "client"
    return "other"

# ✅ API для дашборда - роль пользователя
@app.route('/api/user/role', methods=['GET'])
def api_user_role():
    """🔐 Возвращает роль текущего пользователя для фронтенда"""
    if not g.user or not g.user.is_authenticated:
        return jsonify({"role": "guest"})
    
    return jsonify({
        "role": g.user.role.name if g.user.role else "user"
        "user_id": g.user.id
    })

# Страница метрик
@app.route('/metrics')
def metrics_page():
    return render_template('metrics.html')

# ---------------- SEED DATA ----------------
def seed_roles_and_admin():
    # Роли уже существуют в БД, пропускаем создание
    pass

    # Проверяем, есть ли admin пользователь
    if not User.query.filter_by(username='admin').first():
        admin_role = Role.query.filter_by(name='Администратор').first()
        if admin_role:
            hashed = _hash_password('admin123')
            db.session.add(User(username='admin', name='Администратор', email='admin@local.local', password=hashed, role_id=admin_role.id))
            db.session.commit()
            log.info("admin_user_created", username="admin", password="admin123")

# ---------------- ROUTES ✅ ПОЛНЫЙ СПИСОК ----------------
@app.route('/')
def index():
    if not current_user.is_authenticated:
        return redirect(url_for('login_page'))
    return render_template('index.html')

@app.route('/login')
def login_page():
    return render_template('login.html')

@app.route('/register')
def register_page():
    return render_template('register.html')

# ✅ ЛОГОУТ - КРИТИЧЕСКИ ВАЖНО!
@app.route('/logout')
def logout_page():
    """🚪 Выход с редиректом на логин"""
    session.pop('user_id', None)
    return redirect(url_for('login_page'))

# ✅ АДМИН ДАШБОРД
# (Удалён - можно добавить при необходимости)

@app.route('/users')
@role_required("Администратор")
def users_page():
    return render_template('users.html')

@app.route('/roles')
@role_required("Администратор")
def roles_page():
    return render_template('roles.html')

# ---------------- AUTH API ----------------
@app.route('/api/register', methods=['POST'])
def register():
    # Создание профилей сотрудников доступно только тех-админу.
    if not current_user.is_authenticated or not current_user.role or current_user.role.name != 'Администратор':
        return jsonify({"error": "Только тех-администратор может создавать профили сотрудников"}), 403

    data = request.get_json()
    
    # ✅ Валидация с помощью Marshmallow
    errors = user_schema.validate(data)
    if errors:
        error_messages = []
        for field, messages in errors.items():
            if isinstance(messages, list):
                error_messages.append(f"{field}: {', '.join(messages)}")
            else:
                error_messages.append(f"{field}: {messages}")
        log.warning("validation_error", endpoint="register", errors=error_messages)
        return jsonify({"error": "Ошибка валидации", "details": "; ".join(error_messages)}), 400
    
    validated_data = user_schema.load(data)
    username = validated_data.get('username', '').strip().lower()
    
    if User.query.filter(db.func.lower(User.username) == username).first():
        log.warning("registration_failed", reason="user_exists", username=username)
        return jsonify({"error": "Пользователь уже существует"}), 400
    employee_role = Role.query.filter_by(name='Сотрудник').first()
    user = User(
        username=username,
        email=validated_data.get('email'),
        name=validated_data.get('name', username),
        password=_hash_password(validated_data['password']),
        role=employee_role
    )
    db.session.add(user)
    db.session.commit()
    log.info("user_registered", user_id=user.id, username=username)
    return jsonify({"message": "Регистрация успешна"}), 201

@app.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    
    # ✅ Валидация с помощью Marshmallow
    errors = login_schema.validate(data)
    if errors:
        error_messages = []
        for field, messages in errors.items():
            if isinstance(messages, list):
                error_messages.append(f"{field}: {', '.join(messages)}")
            else:
                error_messages.append(f"{field}: {messages}")
        log.warning("validation_error", endpoint="login", errors=error_messages)
        return jsonify({"error": "Ошибка валидации", "details": "; ".join(error_messages)}), 400
    
    validated_data = login_schema.load(data)
    username = validated_data.get('username', '').strip().lower()
    password = validated_data.get('password', '')
    
    user = User.query.filter(db.func.lower(User.username) == username).first()
    if not user:
        log.warning("login_failed", reason="user_not_found", username=username)
        return jsonify({"error": "Пользователь не найден"}), 401
    if not _check_password(password, user.password):
        log.warning("login_failed", reason="invalid_password", username=username)
        return jsonify({"error": "Неверный логин или пароль"}), 401
    login_user(user)
    user.last_login = datetime.utcnow()
    db.session.commit()
    log.info("user_logged_in", user_id=user.id, username=username)
    return jsonify({"message": "Добро пожаловать!", "user": user.id, "role": user.role.name})

@app.route('/api/logout', methods=['POST'])
@login_required
def api_logout():
    """🔄 AJAX logout для API"""
    username = current_user.username if current_user.is_authenticated else "unknown"
    logout_user()
    log.info("user_logged_out", username=username)
    return jsonify({"message": "Выход выполнен"}), 200

# ---------------- USERS API ----------------
@app.route('/api/users')
@role_required("Администратор")
def api_get_users():
    users = User.query.all()
    payload = []
    for u in users:
        tg = _get_telegram_link_by_email(u.email) or {}
        payload.append({
            "id": u.id,
            "name": u.name,
            "email": u.email,
            "role": u.role.name if u.role else None,
            "is_active": u.is_active,
            "user_type": _user_type_by_role(u.role.name if u.role else None),
            "created_at": u.created_at.isoformat() if u.created_at else None,
            "last_login": u.last_login.isoformat() if u.last_login else None,
            "telegram_confirmed": bool(tg.get("is_verified", False)),
            "telegram_chat_id": tg.get("telegram_chat_id"),
            "telegram_username": tg.get("telegram_username"),
        })
    return jsonify(payload)


@app.route('/api/staff', methods=['POST'])
@role_required("Администратор")
def create_staff_user():
    data = request.get_json(silent=True) or {}
    username = str(data.get("username") or "").strip().lower()
    password = str(data.get("password") or "").strip()
    name = str(data.get("name") or username).strip()
    email = str(data.get("email") or "").strip().lower()

    if not username or len(username) < 3:
        return jsonify({"error": "Логин должен содержать минимум 3 символа"}), 400
    if len(password) < 6:
        return jsonify({"error": "Пароль должен содержать минимум 6 символов"}), 400

    if User.query.filter(db.func.lower(User.username) == username).first():
        return jsonify({"error": "Пользователь с таким логином уже существует"}), 400

    role_name = str(data.get("role") or "Сотрудник").strip()
    role = Role.query.filter_by(name=role_name).first()
    if not role:
        role = Role.query.filter_by(name="Сотрудник").first()
    if not role:
        role = Role(name="Сотрудник", permissions={})
        db.session.add(role)
        db.session.flush()

    user = User(
        username=username,
        name=name if name else username,
        email=email or None,
        password=_hash_password(password),
        role=role,
    )
    db.session.add(user)
    db.session.commit()

    return jsonify({
        "message": "Профиль сотрудника создан",
        "user": {
            "id": user.id,
            "username": user.username,
            "name": user.name,
            "email": user.email,
            "role": user.role.name if user.role else None,
            "user_type": _user_type_by_role(user.role.name if user.role else None),
        }
    }), 201

@app.route('/api/users/<int:uid>/role', methods=['POST'])
@role_required("Администратор")
def update_user_role(uid):
    data = request.get_json()
    user = User.query.get_or_404(uid)
    role_name = data.get("role")

    role = Role.query.filter_by(name=role_name).first()
    if not role:
        return jsonify({"error": f"Роль '{role_name}' не найдена"}), 404

    user.role = role
    db.session.commit()

    return jsonify({
        "message": f"Роль пользователя {user.name} успешно обновлена на '{role.name}'"
    }), 200



@app.route('/api/users/<int:uid>', methods=['DELETE'])
@role_required("Администратор")
def delete_user(uid):
    user = User.query.get_or_404(uid)

    if user.role and user.role.name == 'Администратор':
        return jsonify({"error": "Нельзя удалить администратора"}), 403

    if user.id == current_user.id:
        return jsonify({"error": "Нельзя удалить самого себя"}), 403

    visits_count = Visit.query.filter_by(user_id=user.id).count()
    logs_count = Log.query.filter_by(user_id=user.id).count()

    try:
        if visits_count > 0 or logs_count > 0:
            Visit.query.filter_by(user_id=user.id).update({"user_id": None})
            db.session.query(Log).filter(Log.user_id == user.id).delete()
            db.session.delete(user)
            db.session.commit()
            return jsonify({
                "message": f"Пользователь '{user.name}' удалён. Также удалено записей: посещений {visits_count}, логов {logs_count}."
            }), 200
        else:
            db.session.delete(user)
            db.session.commit()
            return jsonify({"message": f"Пользователь '{user.name}' успешно удалён"}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": f"Ошибка удаления: {str(e)}"}), 500

@app.route('/api/users/<int:uid>/block', methods=['POST'])
@role_required("Администратор")
def toggle_block_user(uid):
    user = User.query.get_or_404(uid)

    if user.role and user.role.name == 'Администратор':
        return jsonify({"error": "Нельзя заблокировать администратора"}), 403

    if user.id == current_user.id:
        return jsonify({"error": "Нельзя заблокировать самого себя"}), 403

    user.is_active = not user.is_active
    db.session.commit()

    status = "заблокирован" if not user.is_active else "разблокирован"
    return jsonify({"message": f"Пользователь '{user.name}' {status}", "is_active": user.is_active}), 200


# ---------------- ROLES API ----------------
@app.route('/api/roles', methods=['GET', 'POST'])
@role_required("Администратор")
def api_roles():
    if request.method == 'GET':
        roles = Role.query.all()
        return jsonify([
            {"id": r.id, "name": r.name, "permissions": r.permissions}
            for r in roles
        ])
    
    if request.method == 'POST':
        data = request.get_json()
        
        # ✅ Валидация с помощью Marshmallow
        errors = role_schema.validate(data)
        if errors:
            error_messages = []
            for field, messages in errors.items():
                if isinstance(messages, list):
                    error_messages.append(f"{field}: {', '.join(messages)}")
                else:
                    error_messages.append(f"{field}: {messages}")
            log.warning("validation_error", endpoint="create_role", errors=error_messages)
            return jsonify({"error": "Ошибка валидации", "details": "; ".join(error_messages)}), 400
        
        validated_data = role_schema.load(data)
        name = validated_data.get("name")
        permissions = validated_data.get("permissions", {})

        if Role.query.filter_by(name=name).first():
            log.warning("role_creation_failed", reason="role_exists", name=name)
            return jsonify({"error": "Такая роль уже существует"}), 400

        role = Role(name=name, permissions=permissions)
        db.session.add(role)
        db.session.commit()

        log.info("role_created", role_id=role.id, name=name)
        return jsonify({"message": f"Роль '{name}' успешно добавлена"}), 201

@app.route('/api/roles/<int:role_id>', methods=['DELETE'])
@role_required("Администратор")
def delete_role(role_id):
    role = Role.query.get(role_id)
    if not role:
        log.warning("role_not_found", role_id=role_id)
        return jsonify({"error": "Роль не найдена"}), 404
    users_with_role = User.query.filter_by(role_id=role.id).count()
    if users_with_role > 0:
        log.warning("role_delete_failed", reason="users_exist", role_id=role_id, users_count=users_with_role)
        return jsonify({"error": "Нельзя удалить роль, пока есть пользователи с этой ролью"}), 400
    db.session.delete(role)
    db.session.commit()
    log.info("role_deleted", role_id=role_id, name=role.name)
    return jsonify({"message": "Роль успешно удалена"})

# ---------------- METRICS API ----------------
@app.route('/api/metrics')
def api_metrics_summary():
    total_users = User.query.count()
    total_visits = Visit.query.count()
    today = datetime.utcnow().date()
    active_today = Visit.query.filter(func.date(Visit.timestamp) == today).count()
    total_logs = Log.query.count()

    return jsonify({
        "users": total_users,
        "visits": total_visits,
        "active_today": active_today,
        "logs": total_logs
    })

@app.route('/api/metrics/daily')
def api_metrics_daily():
    start_str = request.args.get('start')
    end_str = request.args.get('end')

    q = db.session.query(
        func.date(Visit.timestamp).label("day"),
        func.count(Visit.id).label("count")
    ).group_by("day").order_by("day")

    if start_str:
        try:
            start_date = datetime.strptime(start_str, '%Y-%m-%d').date()
            q = q.filter(func.date(Visit.timestamp) >= start_date)
        except ValueError:
            log.warning("invalid_date_format", field="start", value=start_str)

    if end_str:
        try:
            end_date = datetime.strptime(end_str, '%Y-%m-%d').date()
            end_date_next = date(end_date.year, end_date.month, end_date.day + 1)
            q = q.filter(func.date(Visit.timestamp) < end_date_next)
        except ValueError:
            log.warning("invalid_date_format", field="end", value=end_str)

    rows = q.all()
    labels = [str(r.day) for r in rows]
    visits = [int(r.count) for r in rows]
    activity = visits.copy()

    return jsonify({
        "labels": labels,
        "series": [
            {"name": "visits", "data": visits},
            {"name": "activity", "data": activity},
        ],
        "visits": visits,
        "activity": activity
    })

@app.route('/api/metrics/activity')
def api_metrics_activity():
    role = request.args.get('role')
    user_id = request.args.get('user_id', type=int)
    page = request.args.get('page')

    q = db.session.query(
        func.date(Visit.timestamp).label("day"),
        func.count(Visit.id).label("count")
    ).outerjoin(User, Visit.user_id == User.id)

    if role:
        q = q.outerjoin(Role, User.role_id == Role.id).filter(Role.name == role)
    if user_id:
        q = q.filter(Visit.user_id == user_id)
    if page:
        q = q.filter(Visit.page.ilike(f"%{page}%"))

    rows = q.group_by("day").order_by("day").all()
    labels = [str(r.day) for r in rows]
    counts = [int(r.count) for r in rows]

    return jsonify({
        "labels": labels,
        "series": [{"name": "activity", "data": counts}],
        "counts": counts
    })

@app.route('/api/metrics/heatmap')
def api_metrics_heatmap():
    start_str = request.args.get('start')
    end_str = request.args.get('end')

    matrix = [[0 for _ in range(24)] for _ in range(7)]

    q = db.session.query(
        func.extract('dow', Visit.timestamp).label("weekday"),
        func.extract('hour', Visit.timestamp).label("hour"),
        func.count(Visit.id).label("count")
    ).group_by("weekday", "hour")

    if start_str:
        try:
            start_date = datetime.strptime(start_str, '%Y-%m-%d').date()
            q = q.filter(func.date(Visit.timestamp) >= start_date)
        except ValueError:
            log.warning("invalid_date_format_heatmap", field="start", value=start_str)

    if end_str:
        try:
            end_date = datetime.strptime(end_str, '%Y-%m-%d').date()
            end_date_next = date(end_date.year, end_date.month, end_date.day + 1)
            q = q.filter(func.date(Visit.timestamp) < end_date_next)
        except ValueError:
            log.warning("invalid_date_format_heatmap", field="end", value=end_str)

    rows = q.all()

    for r in rows:
        w = int(r.weekday)
        h = int(r.hour)
        w = (w + 6) % 7
        matrix[w][h] = int(r.count)

    return jsonify(matrix)

@app.route('/api/metrics/ops')
def api_metrics_ops():
    ops = _compute_ops_snapshot(hours_window=24)
    error_rate = ops["error_rate"]
    e4xx = ops["errors_4xx"]
    e5xx = ops["errors_5xx"]
    latency = ops["latency"]

    return jsonify({
        "error_rate": error_rate,
        "errors_4xx": e4xx,
        "errors_5xx": e5xx,
        "latency": latency,
        "latency_samples": ops["latency_samples"],
        "integrations": [
            {"name": "Java API", "status": "ok"},
            {"name": "Node API", "status": "ok"},
            {"name": "SMTP", "status": "ok"},
            {"name": "AI Provider", "status": "degraded" if e5xx > 0 else "ok"},
        ],
        "alerts": [
            {"level": "critical", "message": "Резкий рост 5xx" if e5xx > 20 else "Нет критичных всплесков 5xx"},
            {"level": "warning", "message": "Пустые latency-данные" if ops["latency_samples"] < 5 else "Latency-данных достаточно"},
        ]
    })

@app.route('/api/metrics/alerts')
def api_metrics_alerts():
    ops = _compute_ops_snapshot(hours_window=24)
    history_rows = db.session.execute(text("""
        SELECT date_trunc('hour', timestamp) AS hr,
               SUM(CASE WHEN lower(action) LIKE '%error%' OR lower(action) LIKE '%5xx%' THEN 1 ELSE 0 END) AS err5xx,
               COUNT(*) AS total
        FROM logs
        WHERE timestamp >= NOW() - INTERVAL '24 hours'
        GROUP BY hr
        ORDER BY hr DESC
        LIMIT 24
    """)).mappings().all()

    alerts = []
    if ops["errors_5xx"] > 20:
        alerts.append({"level": "critical", "rule": "high_5xx", "message": "За 24 часа зафиксирован высокий уровень 5xx."})
    if ops["latency"]["p95"] is not None and ops["latency"]["p95"] > 1200:
        alerts.append({"level": "warning", "rule": "high_p95", "message": "Latency p95 превышает 1200 ms."})
    if ops["latency_samples"] < 5:
        alerts.append({"level": "warning", "rule": "low_latency_samples", "message": "Недостаточно замеров latency для надежной оценки."})
    if not alerts:
        alerts.append({"level": "ok", "rule": "stable", "message": "Критичных алертов не обнаружено."})

    history = [
        {
            "hour": str(r["hr"]),
            "errors_5xx_like": int(r["err5xx"] or 0),
            "total_logs": int(r["total"] or 0),
        }
        for r in history_rows
    ]
    return jsonify({
        "current": alerts,
        "history": history
    })

@app.route('/api/metrics/funnel')
def api_metrics_funnel():
    # Funnel из analytics_events (если таблица недоступна — безопасный fallback).
    try:
        rows = db.session.execute(text("""
            SELECT event_type, COUNT(*) AS cnt
            FROM analytics_events
            GROUP BY event_type
        """)).mappings().all()
        counts = {str(r["event_type"]): int(r["cnt"]) for r in rows}
    except Exception:
        counts = {}

    visits = int(counts.get("visit", 0) or counts.get("page_view", 0) or Visit.query.count())
    poi_views = int(counts.get("poi_view", 0))
    route_starts = int(counts.get("route_start", 0) + counts.get("route_started", 0))
    orders = int(counts.get("order_created", 0))
    payments = int(counts.get("payment_success", 0) + counts.get("order_paid", 0))

    return jsonify({
        "labels": ["Визиты", "POI просмотры", "Запуск маршрута", "Заказ", "Оплата"],
        "series": [{"name": "funnel", "data": [visits, poi_views, route_starts, orders, payments]}],
        "steps": {
            "visits": visits,
            "poi_views": poi_views,
            "route_starts": route_starts,
            "orders": orders,
            "payments": payments,
        }
    })

@app.route('/api/metrics/data-quality')
def api_metrics_data_quality():
    total_orders = db.session.execute(text("SELECT COUNT(*) AS c FROM orders")).scalar() or 0
    bad_contacts = db.session.execute(text("""
        SELECT COUNT(*) AS c
        FROM orders
        WHERE (email IS NULL OR trim(email) = '')
           OR (phone IS NULL OR trim(phone) = '')
    """)).scalar() or 0
    stuck_status = db.session.execute(text("""
        SELECT COUNT(*) AS c
        FROM orders
        WHERE status IN ('PROCESSING','CONFIRMED')
          AND created_at < NOW() - INTERVAL '72 hours'
    """)).scalar() or 0
    poi_missing_media = db.session.execute(text("""
        SELECT COUNT(*) AS c
        FROM points_of_interest
        WHERE latitude IS NULL OR longitude IS NULL OR (image_url IS NULL AND image_data IS NULL)
    """)).scalar() or 0

    return jsonify({
        "orders_total": int(total_orders),
        "orders_missing_contacts": int(bad_contacts),
        "orders_missing_contacts_pct": round((bad_contacts / total_orders) * 100, 2) if total_orders else 0,
        "orders_stuck_status": int(stuck_status),
        "poi_missing_coordinates_or_image": int(poi_missing_media),
    })

# ---------------- REPORTS ----------------
try:
    from appl.reports.generate_report import bp as reports_bp
    app.register_blueprint(reports_bp, url_prefix='/api/reports')
    log.info("Reports blueprint registered successfully", url_prefix="/api/reports")
except Exception as e:
    import traceback
    log.error(
        "reports_blueprint_failed",
        module="reports",
        error=str(e),
        traceback=traceback.format_exc(),
    )

# ------------------ APP START ------------------
if __name__ == '__main__':
    with app.app_context():
        db.create_all()  # Создаём таблицы
        seed_roles_and_admin()
    # Используем threaded=True для Windows и отключаем reloader
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)
