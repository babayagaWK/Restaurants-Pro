import requests
import time

TOKEN = "2ab33f093f36707ddc4825dff5205ec7151ee8dd"
USERNAME = "Siripong"
DOMAIN = "Siripong.pythonanywhere.com"

headers = {'Authorization': f'Token {TOKEN}'}
base_url = f"https://www.pythonanywhere.com/api/v0/user/{USERNAME}"

payload = """import sqlite3
import traceback

try:
    db_path = '/home/Siripong/backend/db.sqlite3'
    print("DB:", db_path)
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("ALTER TABLE core_category ADD COLUMN is_active bool DEFAULT 1 NOT NULL;")
    conn.commit()
    print("Success")
except Exception as e:
    print("DB Exception:", e)

print("FINISHED_SQLITE")
"""

with open("temp2.py", "w") as f:
    f.write(payload)

with open("temp2.py", "rb") as f:
    requests.post(f"{base_url}/files/path/home/{USERNAME}/backend/pa_sqlite_test.py", files={'content': f}, headers=headers)

resp = requests.post(f"{base_url}/consoles/", json={"executable": "bash"}, headers=headers)
console_id = resp.json()['id']

requests.post(f"{base_url}/consoles/{console_id}/send_input/", json={"input": "python3 -u ~/backend/pa_sqlite_test.py\n"}, headers=headers)

for i in range(5):
    time.sleep(3)
    resp = requests.get(f"{base_url}/consoles/{console_id}/get_latest_output/", headers=headers)
    out = resp.json().get('output', '')
    if 'FINISHED_SQLITE' in out:
        break

print("=== CONSOLE OUTPUT ===")
print(out)
print("======================")

requests.post(f"{base_url}/webapps/{DOMAIN}/reload/", headers=headers)
requests.delete(f"{base_url}/consoles/{console_id}/", headers=headers)
