"""
Схемы валидации данных с использованием Marshmallow
"""
from flask_marshmallow import Marshmallow
from marshmallow import Schema, fields, validate, validates, ValidationError
from appl.extensions import db

ma = Marshmallow()

# ---------------- Валидация пользователей ----------------

class UserSchema(Schema):
    """Схема для валидации данных пользователя"""
    id = fields.Int(dump_only=True)
    username = fields.Str(required=True, validate=validate.Length(min=3, max=50))
    email = fields.Email(required=False, allow_none=True)
    name = fields.Str(required=False, allow_none=True, validate=validate.Length(max=100))
    password = fields.Str(required=True, validate=validate.Length(min=6, max=128), load_only=True)
    role_id = fields.Int(required=False, allow_none=True)
    created_at = fields.DateTime(dump_only=True)
    last_login = fields.DateTime(dump_only=True)


class LoginSchema(Schema):
    """Схема для валидации данных входа"""
    username = fields.Str(required=True, validate=validate.Length(min=1, max=100))
    password = fields.Str(required=True, validate=validate.Length(min=1, max=128))


class UserUpdateSchema(Schema):
    """Схема для обновления пользователя"""
    email = fields.Email(required=False, allow_none=True)
    name = fields.Str(required=False, allow_none=True, validate=validate.Length(max=100))
    role_id = fields.Int(required=False, allow_none=True)


# ---------------- Валидация ролей ----------------

class RoleSchema(Schema):
    """Схема для валидации данных роли"""
    id = fields.Int(dump_only=True)
    name = fields.Str(required=True, validate=validate.Length(min=1, max=50))
    permissions = fields.Dict(required=False, load_default={})


class RoleUpdateSchema(Schema):
    """Схема для обновления роли"""
    name = fields.Str(required=False, validate=validate.Length(min=1, max=50))
    permissions = fields.Dict(required=False)


# ---------------- Валидация отчётов ----------------

class ReportSchema(Schema):
    """Схема для генерации отчёта"""
    start_date = fields.Date(required=True)
    end_date = fields.Date(required=True)
    report_type = fields.Str(required=False, validate=validate.OneOf(['visits', 'users', 'logs', 'full']))


# Создаём экземпляры схем для использования в коде
user_schema = UserSchema()
users_schema = UserSchema(many=True)
login_schema = LoginSchema()
user_update_schema = UserUpdateSchema()
role_schema = RoleSchema()
roles_schema = RoleSchema(many=True)
role_update_schema = RoleUpdateSchema()
report_schema = ReportSchema()
