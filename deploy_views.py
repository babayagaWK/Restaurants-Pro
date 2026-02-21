import requests
import json

TOKEN = "2ab33f093f36707ddc4825dff5205ec7151ee8dd"
USERNAME = "Siripong"
DOMAIN = "Siripong.pythonanywhere.com"

headers = {'Authorization': f'Token {TOKEN}'}
base_url = f"https://www.pythonanywhere.com/api/v0/user/{USERNAME}"

print("Uploading views.py to PythonAnywhere...")
with open("backend/core/views.py", "rb") as f:
    resp = requests.post(f"{base_url}/files/path/home/{USERNAME}/backend/core/views.py", files={'content': f}, headers=headers)
print("Upload status:", resp.status_code)

print("Reloading WebApp...")
reload_resp = requests.post(f"{base_url}/webapps/{DOMAIN}/reload/", headers=headers)
print("Reload status:", reload_resp.status_code)
print("Done")
