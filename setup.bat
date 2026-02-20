@echo off
echo ==========================================
echo Restaurant POS MVP Setup Script
echo ==========================================

cd /d "%~dp0"

echo.
echo [1/3] Installing Python dependencies...
python -m pip install -r backend/requirements.txt
if %errorlevel% neq 0 (
    echo Error installing dependencies. Please check your Python installation.
    pause
    exit /b %errorlevel%
)

echo.
echo [2/3] Creating Database Migrations...
python backend/manage.py makemigrations
if %errorlevel% neq 0 (
    echo Error creating migrations.
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] Applying Migrations...
python backend/manage.py migrate
if %errorlevel% neq 0 (
    echo Error applying migrations.
    pause
    exit /b %errorlevel%
)

echo.
echo ==========================================
echo Setup Complete!
echo You can now run the server with:
echo python backend/manage.py runserver
echo ==========================================
pause
