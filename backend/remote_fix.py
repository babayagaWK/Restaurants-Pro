import requests
import time

TOKEN = "2ab33f093f36707ddc4825dff5205ec7151ee8dd"
USERNAME = "Siripong"
DOMAIN = "Siripong.pythonanywhere.com"

headers = {'Authorization': f'Token {TOKEN}'}
base_url = f"https://www.pythonanywhere.com/api/v0/user/{USERNAME}"

payload = """import sqlite3
import os

db_path = '/home/Siripong/backend/db.sqlite3'
print("Checking DB:", db_path)

conn = sqlite3.connect(db_path)
cur = conn.cursor()

try:
    cur.execute("ALTER TABLE core_category ADD COLUMN is_active bool DEFAULT 1 NOT NULL;")
    print("Added is_active")
except Exception as e:
    print("Error:", e)

try:
    cur.execute("ALTER TABLE core_menuitem ADD COLUMN image varchar(100) DEFAULT '';")
    print("Added image")
except Exception as e:
    print("Error:", e)

try:
    cur.execute('CREATE TABLE "core_sitesettings" ("id" integer NOT NULL PRIMARY KEY AUTOINCREMENT, "background_image" varchar(100) NULL, "blur_amount" integer NOT NULL);')
    print("Created core_sitesettings")
except Exception as e:
    print("Error creating sitesettings:", e)

conn.commit()

try:
    cur.execute("INSERT OR IGNORE INTO django_migrations (app, name, applied) VALUES ('core', '0002_sitesettings_alter_category_options_and_more', datetime('now'));")
    conn.commit()
    print("Faked migration record.")
except Exception as e:
    print("Error faking:", e)

conn.close()
print("FINISHED_SQLITE")
"""

with open("temp_fix.py", "w") as f:
    f.write(payload)

print("Uploading to PythonAnywhere...")
with open("temp_fix.py", "rb") as f:
    resp = requests.post(f"{base_url}/files/path/home/{USERNAME}/backend/pa_sqlite_fix.py", files={'content': f}, headers=headers)
print("Upload status:", resp.status_code)

print("Creating console...")
resp = requests.post(f"{base_url}/consoles/", json={"executable": "bash"}, headers=headers)
console_id = resp.json()['id']

print("Running script via bash...")
requests.post(f"{base_url}/consoles/{console_id}/send_input/", json={"input": "python3 -u ~/backend/pa_sqlite_fix.py\n"}, headers=headers)

for i in range(15):
    time.sleep(3)
    resp = requests.get(f"{base_url}/consoles/{console_id}/get_latest_output/", headers=headers)
    out = resp.json().get('output', '')
    if 'FINISHED_SQLITE' in out:
        print("\nFINAL OUTPUT:")
        print(out)
        break
    print(".", end="", flush=True)

print("\nReloading WebApp...")
reload_resp = requests.post(f"{base_url}/webapps/{DOMAIN}/reload/", headers=headers)
print("Reload status:", reload_resp.status_code)

requests.delete(f"{base_url}/consoles/{console_id}/", headers=headers)
import os
os.remove("temp_fix.py")
print("Done")
