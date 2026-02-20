
import os
import sys
import zipfile
import json
import threading
import time
import webbrowser
import shutil
from datetime import datetime
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext, simpledialog
import traceback

# Try importing requests
try:
    import requests
except ImportError:
    messagebox.showerror("Missing Dependency", "Please run 'pip install requests' to use this tool.")
    sys.exit()

CONFIG_FILE = "pa_config.json"
BACKEND_DIR = "backend"
ZIP_NAME = "deploy_package.zip"

class UpdateManagerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("FoodPOS Update Manager 🚀 (v2.0)")
        self.root.geometry("600x750")
        self.root.configure(bg="#f0f2f5")

        # Config
        self.config = self.load_config()
        self.username = tk.StringVar(value=self.config.get("username", "Siripong"))
        self.token = tk.StringVar(value=self.config.get("token", ""))
        self.domain = tk.StringVar(value=self.config.get("domain", "Siripong.pythonanywhere.com"))

        self.setup_ui()

    def load_config(self):
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, "r") as f:
                    return json.load(f)
            except:
                return {}
        return {}

    def save_config(self):
        config = {
            "username": self.username.get(),
            "token": self.token.get(),
            "domain": self.domain.get()
        }
        with open(CONFIG_FILE, "w") as f:
            json.dump(config, f)
            messagebox.showinfo("Saved", "Settings saved successfully!")

    def setup_ui(self):
        style = ttk.Style()
        style.theme_use('clam')
        style.configure("TButton", padding=6, relief="flat", background="#007bff", foreground="white")
        style.map("TButton", background=[("active", "#0056b3")])

        # Header
        header = tk.Frame(self.root, bg="#1a1a2e", height=80)
        header.pack(fill="x")
        tk.Label(header, text="FoodPOS Deployment Center", font=("Segoe UI", 18, "bold"), bg="#1a1a2e", fg="white").pack(pady=20)

        # Settings Frame
        settings_frame = ttk.LabelFrame(self.root, text="PythonAnywhere Settings", padding=15)
        settings_frame.pack(fill="x", padx=20, pady=10)

        grid_opts = {"sticky": "w", "pady": 5}
        
        ttk.Label(settings_frame, text="Username:").grid(row=0, column=0, **grid_opts)
        ttk.Entry(settings_frame, textvariable=self.username, width=30).grid(row=0, column=1, **grid_opts)

        ttk.Label(settings_frame, text="API Token:").grid(row=1, column=0, **grid_opts)
        ttk.Entry(settings_frame, textvariable=self.token, width=50, show="*").grid(row=1, column=1, **grid_opts)
        
        help_btn = tk.Label(settings_frame, text="open API Token page", fg="blue", cursor="hand2")
        help_btn.grid(row=1, column=2, padx=5)
        help_btn.bind("<Button-1>", lambda e: webbrowser.open(f"https://www.pythonanywhere.com/user/{self.username.get()}/account/#api_token"))

        ttk.Label(settings_frame, text="Domain:").grid(row=2, column=0, **grid_opts)
        ttk.Entry(settings_frame, textvariable=self.domain, width=30).grid(row=2, column=1, **grid_opts)

        ttk.Button(settings_frame, text="💾 Save Config", command=self.save_config).grid(row=3, column=1, sticky="e", pady=10)

        # Actions Frame
        action_frame = ttk.LabelFrame(self.root, text="Deployment Actions", padding=15)
        action_frame.pack(fill="x", padx=20, pady=10)

        self.btn_deploy = ttk.Button(action_frame, text="🚀 One-Click Auto Deploy (Recommended)", command=self.start_auto_deploy)
        self.btn_deploy.pack(fill="x", pady=5)
        
        ttk.Button(action_frame, text="📦 Just Pack Zip (Manual Upload)", command=self.pack_zip_only).pack(fill="x", pady=5)
        ttk.Button(action_frame, text="🔍 Verify Remote File (Debug)", command=self.check_remote_file_thread).pack(fill="x", pady=5)

        # Log Area
        self.log_area = scrolledtext.ScrolledText(self.root, height=15, state="disabled", font=("Consolas", 9))
        self.log_area.pack(fill="both", expand=True, padx=20, pady=(0, 20))

    def log(self, message):
        def _update():
            self.log_area.config(state="normal")
            self.log_area.insert(tk.END, f"[{datetime.now().strftime('%H:%M:%S')}] {message}\n")
            self.log_area.see(tk.END)
            self.log_area.config(state="disabled")
        self.root.after(0, _update)

    def pack_zip(self):
        self.log("📦 Zipping files...")
        if not os.path.exists(BACKEND_DIR):
            self.log("❌ Error: 'backend' directory not found!")
            return None

        try:
            file_count = 0
            with zipfile.ZipFile(ZIP_NAME, 'w', zipfile.ZIP_DEFLATED) as zipf:
                for root, dirs, files in os.walk(BACKEND_DIR):
                    # Filter folders
                    dirs[:] = [d for d in dirs if d not in ['__pycache__', 'venv', '.git', 'env']]
                    
                    # Exclude collected static root only, keep app static
                    if root == BACKEND_DIR and 'static' in dirs:
                        dirs.remove('static')
                    
                    for file in files:
                        if file in ['db.sqlite3', '.DS_Store', '*.pyc'] or file.endswith('.pyc'):
                            continue
                        
                        file_path = os.path.join(root, file)
                        arcname = os.path.relpath(file_path, BACKEND_DIR)
                        zipf.write(file_path, arcname)
                        file_count += 1
            
            size_mb = os.path.getsize(ZIP_NAME) / (1024 * 1024)
            self.log(f"✅ Zip created: {ZIP_NAME} ({size_mb:.2f} MB, {file_count} files)")
            
            if file_count < 5:
                self.log("⚠️ WARNING: Zip has very few files! Check 'backend' folder.")
                
            return ZIP_NAME
        except Exception as e:
            self.log(f"❌ Zip failed: {str(e)}")
            return None

    def task_wrapper(self, task_func):
        self.btn_deploy.config(state="disabled")
        threading.Thread(target=task_func, daemon=True).start()

    def pack_zip_only(self):
        self.task_wrapper(lambda: self._pack_only())

    def _pack_only(self):
        if self.pack_zip():
            messagebox.showinfo("Success", f"Package created: {ZIP_NAME}\nYou can now upload this file manually.")
        self.btn_deploy.config(state="normal")

    def check_remote_file_thread(self):
        threading.Thread(target=self.check_remote_file, daemon=True).start()

    def check_remote_file(self):
        username = self.username.get()
        token = self.token.get()
        headers = {'Authorization': f'Token {token}'}
        
        file_path = f"/home/{username}/backend/core/templates/index.html"
        url = f"https://www.pythonanywhere.com/api/v0/user/{username}/files/path{file_path}"
        
        self.log(f"🔎 Checking remote file: {file_path}...")
        try:
            resp = requests.get(url, headers=headers, timeout=60)
            if resp.status_code == 200:
                content = resp.text
                if "ยินดีต้อนรับ" in content:
                    self.log("✅ SUCCESS: Remote file contains Thai text. (Server OK)")
                    messagebox.showinfo("Verified", "✅ Server has the latest Thai version.\nIf you still see English, Clear Browser Cache!")
                else:
                    self.log("❌ FAILURE: Remote file still has English text. (Update Failed)")
                    messagebox.showwarning("Verified", "❌ Server still has the old version.")
            else:
                self.log(f"❌ Read failed: HTTP {resp.status_code}")
        except Exception as e:
            self.log(f"❌ Check error: {str(e)}")

    def start_auto_deploy(self):
        if not self.token.get():
            messagebox.showwarning("Missing Token", "Please enter your API Token first!")
            return
        self.task_wrapper(self.auto_deploy_process)

    def auto_deploy_process(self):
        try:
            username = self.username.get()
            token = self.token.get()
            domain = self.domain.get()
            headers = {'Authorization': f'Token {token}'}
            base_url = f"https://www.pythonanywhere.com/api/v0/user/{username}"

            # 1. Zip Code
            zip_file = self.pack_zip()
            if not zip_file:
                return
            
            # 1.5 Generate Python Deploy Script (Safer than Bash)
            script_name = "server_deploy.py"
            script_data = r"""
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
"""
            with open(script_name, "w", encoding="utf-8", newline='\n') as f:
                f.write(script_data)

            # 2. Upload Zip & Script
            self.log("📡 Connecting to PythonAnywhere...")
            requests.get(f"{base_url}/consoles/", headers=headers, timeout=60)

            self.log(f"☁️ Uploading {zip_file}...")
            with open(zip_file, 'rb') as f:
                resp = requests.post(
                    f"{base_url}/files/path/home/{username}/{zip_file}",
                    files={'content': f},
                    headers=headers,
                    timeout=300
                )
                if resp.status_code not in [200, 201]:
                    raise Exception(f"Zip upload failed: {resp.text}")

            self.log(f"☁️ Uploading {script_name}...")
            with open(script_name, 'rb') as f:
                resp = requests.post(
                    f"{base_url}/files/path/home/{username}/{script_name}",
                    files={'content': f},
                    headers=headers,
                    timeout=60
                )
                if resp.status_code not in [200, 201]:
                    raise Exception(f"Script upload failed: {resp.text}")
            
            self.log("✅ Uploads complete.")

            # 3. Clean up OLD Consoles (Fix limit error)
            self.log("🧹 checking for old consoles...")
            try:
                resp = requests.get(f"{base_url}/consoles/", headers=headers, timeout=60)
                if resp.status_code == 200:
                    for console in resp.json():
                        self.log(f"Killing old console {console['id']}...")
                        requests.delete(f"{base_url}/consoles/{console['id']}/", headers=headers, timeout=30)
            except Exception as e:
                self.log(f"⚠️ Warning cleaning consoles: {e}")

            # 4. Execute Script
            self.log("🔧 Initializing deployment console (Timeout set to 60s for Tarpit)...")
            # Increased timeout to 60s because Tarpit makes API very slow
            resp = requests.post(f"{base_url}/consoles/", json={"executable": "bash"}, headers=headers, timeout=60)
            
            if resp.status_code not in [200, 201]:
                raise Exception(f"Console creation failed ({resp.status_code}): {resp.text}")
            
            try:
                console_data = resp.json()
            except Exception as e:
                raise Exception(f"Invalid API Response: {resp.text[:100]}...")

            console_id = console_data['id']
            console_url = f"https://www.pythonanywhere.com/user/{username}/consoles/{console_id}/"
            self.log(f"🔗 Live Console: {console_url}")
            # Prompt user to open it?
            if messagebox.askyesno("Open Console?", "Do you want to watch the deployment live in your browser?"):
                webbrowser.open(console_url)

            # 4.1 Wait for Console to be Ready (Tarpit-proof - Aggressive Mode)
            self.log("⏳ Waiting for console prompt (Sending wake-up signals)...")
            
            # Immediate wake-up
            requests.post(f"{base_url}/consoles/{console_id}/send_input/", json={"input": "\n"}, headers=headers, timeout=60)
            
            console_ready = False
            for i in range(30): # Try for 90 seconds
                time.sleep(3)
                resp = requests.get(f"{base_url}/consoles/{console_id}/get_latest_output/", headers=headers, timeout=60)
                output = resp.json().get('output', '')
                
                # Check for prompt
                if output and ("~ $" in output or "/home/" in output or "bash" in output.lower()):
                    console_ready = True
                    self.log("✅ Console Ready!")
                    break
                
                # Periodic wake-up every 3 attempts (approx 9s)
                if i % 3 == 0:
                    self.log(f"   ...waiting ({i+1}/30) - sending ENTER...")
                    requests.post(f"{base_url}/consoles/{console_id}/send_input/", json={"input": "\n"}, headers=headers, timeout=60)
            
            if not console_ready:
                self.log("⚠️ Console check timed out. Force running script anyway...")

            self.log(f"💻 Running script (ID: {console_id})...")
            # Run with python3 -u (unbuffered) to see output immediately
            requests.post(f"{base_url}/consoles/{console_id}/send_input/", json={"input": f"python3 -u {script_name}\n"}, headers=headers, timeout=60)
            
            # Poll Results
            self.log("⏳ Waiting for script to finish (This may take 10-15 mins)...")
            success = False
            max_checks = 60  # 60 * 5s = 300s (5 minutes)
            for i in range(max_checks):
                time.sleep(5) 
                
                try:
                    resp = requests.get(f"{base_url}/consoles/{console_id}/get_latest_output/", headers=headers, timeout=60)
                    if resp.status_code == 200:
                        output = resp.json().get('output', '')
                        
                        # Streaming Log Logic
                        # Only show new lines that haven't been shown (simple approach: show last non-empty line)
                        lines = [l for l in output.split('\n') if l.strip()]
                        if lines:
                            last_line = lines[-1]
                            # Avoid spamming the same line
                            if last_line != getattr(self, '_last_log_line', ''):
                                self.log(f"   >> {last_line[-80:]}") # Show last 80 chars
                                self._last_log_line = last_line

                        # 1. Success Marker
                        if 'DEPLOYMENT_SUCCESS_MARKER' in output:
                            self.log("✅ Server script execution confirmed!")
                            success = True
                            break
                        
                        # 2. Prompt Re-appearance (Script finished but maybe failed)
                        clean_output = output.strip()
                        if len(clean_output) > 50 and (clean_output.endswith('$') or clean_output.endswith('#') or "~ $" in clean_output[-100:]):
                            if 'python3 -u server_deploy.py' in output:
                                # Check if we actually ran the script and it finished
                                if "Traceback" in output or "Error" in output:
                                     self.log("⚠️ Script finished with potential errors.")
                                     success = False
                                     break
                                else:
                                     # Maybe successful but marker missed?
                                     pass

                except Exception as e:
                    self.log(f"⚠️ Polling warning: {e}")
                    continue
            
            # ALWAYS log output for debugging
            self.log("📝 Console Output Log:")
            self.log("-" * 40)
            self.log(output[-1000:])
            self.log("-" * 40)
            
            if not success:
                self.log("⚠️ Script timeout. Check log above for errors.")
            # Cleanup console
            try:
                requests.delete(f"{base_url}/consoles/{console_id}/", headers=headers, timeout=30)
            except:
                pass

            # 4. Reload WebApp
            self.log("🔄 Reloading WebApp...")
            resp = requests.post(f"{base_url}/webapps/{domain}/reload/", headers=headers, timeout=60)
            if resp.status_code == 200:
                self.log("✅ WebApp Reloaded!")
                messagebox.showinfo("Success", "🎉 Deployment Complete!\nYour site is currently reloading.")
            else:
                self.log(f"❌ Reload failed: {resp.text}")

        except Exception as e:
            self.log(f"❌ CRITICAL ERROR: {str(e)}")
            self.log(traceback.format_exc())
            messagebox.showerror("Error", f"An error occurred:\n{str(e)}")
        finally:
            self.btn_deploy.config(state="normal")

if __name__ == "__main__":
    root = tk.Tk()
    app = UpdateManagerApp(root)
    root.mainloop()
