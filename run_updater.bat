@echo off
echo ==========================================
echo      FoodPOS Update Manager Launcher 🚀
echo ==========================================

echo [1/2] Checking dependencies...
pip install requests > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    - Dependencies OK.
) else (
    echo    - Installing 'requests' library...
    pip install requests
)

echo [2/2] Starting Update Manager...
python update_manager.py

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ The program crashed or was closed with an error.
    pause
)
