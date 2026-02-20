#!/bin/bash
set -e
set -x  # Print commands for debugging

echo "🚀 Starting Deployment..."
echo "📂 PWD: $PWD"

# Target directory
TARGET_DIR="backend"

if [ -f "deploy_package.zip" ]; then
    echo "📦 Extracting Zip to temp folder..."
    rm -rf deploy_temp
    mkdir -p deploy_temp
    unzip -o deploy_package.zip -d deploy_temp
    rm deploy_package.zip
    
    # Check if we have nested backend inside temp
    if [ -d "deploy_temp/backend" ]; then
        echo "⚠️ Detected nested 'backend' folder. Fixing..."
        cp -rf deploy_temp/backend/* $TARGET_DIR/
        # Also copy manage.py/requirements.txt if they are in the temp root but outside nested backend
        [ -f deploy_temp/manage.py ] && cp deploy_temp/manage.py .
        [ -f deploy_temp/requirements.txt ] && cp deploy_temp/requirements.txt .
    else
        echo "✅ Standard structure detected. Syncing..."
        cp -rf deploy_temp/* $TARGET_DIR/
    fi
    
    rm -rf deploy_temp
else
    echo "❌ No Zip Found!"
    exit 1
fi

cd $TARGET_DIR
echo "📂 Entered backend directory: $PWD"
ls -l core/templates/index.html || echo "⚠️ index.html not found!"

echo "⚙️ Static & Migrate..."
python3 manage.py collectstatic --noinput
python3 manage.py makemigrations core
python3 manage.py makemigrations
python3 manage.py migrate
echo "✅ DEPLOYMENT_SUCCESS_MARKER"
