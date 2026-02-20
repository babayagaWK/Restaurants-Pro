import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'pos_project.settings')
django.setup()

from core.models import MenuItem, Category, Order

def clear_data():
    print("🧹 Cleaning up database...")
    
    # Delete Orders first (due to foreign keys)
    deleted_orders, _ = Order.objects.all().delete()
    print(f"❌ Deleted {deleted_orders} orders.")

    # Delete Menu Items
    deleted_items, _ = MenuItem.objects.all().delete()
    print(f"❌ Deleted {deleted_items} menu items.")

    # Delete Categories
    deleted_cats, _ = Category.objects.all().delete()
    print(f"❌ Deleted {deleted_cats} categories.")
    
    print("✅ Database cleared successfully!")

if __name__ == "__main__":
    clear_data()
