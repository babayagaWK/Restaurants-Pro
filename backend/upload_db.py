import requests
import json
import time

TOKEN = "2ab33f093f36707ddc4825dff5205ec7151ee8dd"
USERNAME = "Siripong"
DOMAIN = "Siripong.pythonanywhere.com"

headers = {'Authorization': f'Token {TOKEN}'}
base_url = f"https://www.pythonanywhere.com/api/v0/user/{USERNAME}"

print("Uploading patched DB to PythonAnywhere...")
with open("remote_db.sqlite3", "rb") as f:
    resp = requests.post(f"{base_url}/files/path/home/{USERNAME}/backend/db.sqlite3", files={'content': f}, headers=headers)
print("Upload status:", resp.status_code)

print("Reloading WebApp...")
reload_resp = requests.post(f"{base_url}/webapps/{DOMAIN}/reload/", headers=headers)
print("Reload status:", reload_resp.status_code)
print("Done")
