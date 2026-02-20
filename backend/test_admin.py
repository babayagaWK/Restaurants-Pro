import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'pos_project.settings')
django.setup()

from django.test.client import Client
from django.contrib.auth.models import User

# Create superuser if not exists
if not User.objects.filter(username='admin').exists():
    User.objects.create_superuser('admin', 'admin@example.com', 'admin')

client = Client()
client.login(username='admin', password='admin')

response = client.get('/admin/core/category/')
print("Status Code:", response.status_code)
if response.status_code != 200:
    for tb_line in response.context.get('exception', []):
        pass
    print("Error getting page. Content length:", len(response.content))
    print(response.content.decode('utf-8')[:2000])

response_add = client.get('/admin/core/category/add/')
print("Status Add:", response_add.status_code)

response_change = client.get('/admin/core/category/1/change/')
print("Status Change 1:", response_change.status_code)
