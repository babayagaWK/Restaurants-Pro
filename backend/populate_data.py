import os
import django
import sys

# Add project root to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'pos_project.settings')
django.setup()

from core.models import Category, MenuItem, Order, OrderItem

def run():
    print("Checking for existing data...")
    if Category.objects.exists():
        print("Data already exists. Skipping population.")
        return

    print("Creating categories...")
    food = Category.objects.create(name="Main Course")
    drink = Category.objects.create(name="Beverages")
    dessert = Category.objects.create(name="Desserts")

    print("Creating menu items...")
    MenuItem.objects.create(category=food, name="Cheeseburger", price=150.00, description="Juicy beef burger with cheese", is_available=True)
    MenuItem.objects.create(category=food, name="Pad Thai", price=120.00, description="Classic Thai noodle dish", is_available=True)
    MenuItem.objects.create(category=drink, name="Iced Coffee", price=60.00, description="Refreshing iced coffee", is_available=True)
    MenuItem.objects.create(category=drink, name="Water", price=20.00, description="Mineral water", is_available=True)
    MenuItem.objects.create(category=dessert, name="Mango Sticky Rice", price=100.00, description="Seasonal mango with sticky rice", is_available=True)

    print("Creating sample order...")
    order = Order.objects.create(table_number=5, status='pending')
    OrderItem.objects.create(order=order, menu_item=MenuItem.objects.get(name="Pad Thai"), quantity=2)
    OrderItem.objects.create(order=order, menu_item=MenuItem.objects.get(name="Water"), quantity=2)

    print("Dummy data created successfully!")

if __name__ == "__main__":
    run()
