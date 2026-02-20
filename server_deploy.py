
import os
import sys
import zipfile
import shutil
import subprocess

def log(msg):
    print(msg, flush=True)

log("🚀 Starting Python Deployment...")
PWD = os.getcwd()
log(f"📂 PWD: {PWD}")

ZIP_FILE = "deploy_package.zip"
TARGET_DIR = "backend"
TEMP_DIR = "deploy_temp"

def run_command(cmd):
    log(f"🔧 Running: {cmd}")
    ret = subprocess.call(cmd, shell=True)
    if ret != 0:
        log(f"❌ Command failed: {cmd}")
        # Don't exit, try to continue or cleanup?
        # sys.exit(1)

if os.path.exists(ZIP_FILE):
    log("📦 Found zip file. Extracting...")
    
    if os.path.exists(TEMP_DIR):
        shutil.rmtree(TEMP_DIR)
    os.makedirs(TEMP_DIR)
    
    with zipfile.ZipFile(ZIP_FILE, 'r') as zip_ref:
        zip_ref.extractall(TEMP_DIR)
    
    # Analyze structure
    log("📂 Analyzing extracted structure...")
    nested_backend = os.path.join(TEMP_DIR, "backend")
    source_dir = TEMP_DIR
    
    if os.path.exists(nested_backend) and os.path.isdir(nested_backend):
        log("⚠️ Detected nested 'backend' folder. Adjusting source...")
        source_dir = nested_backend
    
    # Copy files
    log(f"🚚 Copying files from {source_dir} to {TARGET_DIR}...")
    if not os.path.exists(TARGET_DIR):
        os.makedirs(TARGET_DIR)
        
    for item in os.listdir(source_dir):
        s = os.path.join(source_dir, item)
        d = os.path.join(TARGET_DIR, item)
        try:
            if os.path.isdir(s):
                # recursive copy with overwrite
                if os.path.exists(d):
                    shutil.rmtree(d)
                shutil.copytree(s, d)
            else:
                shutil.copy2(s, d)
        except Exception as e:
            log(f"⚠️ Error copying {item}: {e}")
            
    log("✅ Files copied successfully.")
    
    # Cleanup
    shutil.rmtree(TEMP_DIR)
    if os.path.exists(ZIP_FILE):
        os.remove(ZIP_FILE)
    
else:
    log("❌ Zip file not found!")
    sys.exit(1)

# Verify Index Content
index_path = os.path.join(TARGET_DIR, "core", "templates", "index.html")
if os.path.exists(index_path):
    with open(index_path, 'r', encoding='utf-8') as f:
        content = f.read()
        if "ยินดีต้อนรับ" in content:
            log("✅ Verification: 'index.html' contains Thai text.")
        else:
            log("⚠️ Verification: 'index.html' does NOT contain Thai text (English detected?)")
else:
    log(f"❌ '{index_path}' not found!")

# Django Commands
os.chdir(TARGET_DIR)
log(f"📂 Changed to {os.getcwd()}")

log("📦 Installing Dependencies...")
run_command("pip3 install -r requirements.txt --user")

log("⚙️ Collecting Static...")
run_command("python3 manage.py collectstatic --noinput")

log("🗄️ Making Migrations...")
run_command("python3 manage.py makemigrations core")
run_command("python3 manage.py makemigrations")

log("🗄️ Migrating...")
run_command("python3 manage.py migrate")

log("✅ DEPLOYMENT_SUCCESS_MARKER")
