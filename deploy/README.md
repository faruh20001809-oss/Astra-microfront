# Деплой Astra-microfront на сервер (Debian 12)

## Быстрый старт

1. На сервере задайте переменные и запустите первичную установку:

```bash
export GIT_REPO="https://github.com/YOUR_USER/Astra-microfront.git"
sudo bash -c 'curl -sL https://raw.githubusercontent.com/YOUR_USER/Astra-microfront/main/deploy/setup.sh | bash'
```

Или склонируйте репозиторий вручную и запустите из папки проекта:

```bash
git clone https://github.com/YOUR_USER/Astra-microfront.git /opt/astramicro
cd /opt/astramicro
sudo GIT_REPO="" APP_DIR=/opt/astramicro bash deploy/setup.sh
```

Перед запуском отредактируйте `deploy/setup.sh`: укажите свой `GIT_REPO` (URL репозитория).

## Обновление из Git

После правок в репозитории на сервере:

```bash
sudo /opt/astramicro/deploy/update.sh
```

Или из каталога репозитория:

```bash
cd /opt/astramicro
sudo bash deploy/update.sh
```

Скрипт делает: `git pull`, сборку бэкенда (Maven), сборку фронта (npm run build), перезапуск `astrakhan-admin.service`. Статика уже в `dist/`, Nginx перезагружать не нужно.

## Автозапуск

Сервис **astrakhan-admin** (Spring Boot) регистрируется в systemd и включается в автозагрузку:

- Статус: `sudo systemctl status astrakhan-admin`
- Логи: `sudo journalctl -u astrakhan-admin -f`
- Перезапуск: `sudo systemctl restart astrakhan-admin`

## Файлы в deploy/

| Файл | Назначение |
|------|------------|
| `setup.sh` | Первичная установка: пакеты, клон, сборка, systemd, nginx |
| `update.sh` | Обновление: git pull, сборка, перезапуск сервиса |
| `astrakhan-admin.service` | Пример unit для systemd |
| `nginx-astramicro.conf` | Пример конфига Nginx |

## Требования на сервере

- Debian 12
- Доступ в интернет (git, Maven, npm)
- Порт 80 для Nginx (и при необходимости 443 для HTTPS)
