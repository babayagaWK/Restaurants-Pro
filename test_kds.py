import requests
import time
import os

BASE_URL = "http://127.0.0.1:8000/api/api"

def get_orders():
    try:
        response = requests.get(f"{BASE_URL}/orders/?status=pending,cooking", timeout=5)
        if response.status_code == 200:
            return response.json()
        print(f"[API Error] Status {response.status_code}")
    except Exception as e:
        print(f"[Connection Error] Is the Django server running? -> {e}")
    return []

def update_status(order_id, new_status):
    try:
        response = requests.patch(f"{BASE_URL}/orders/{order_id}/", json={"status": new_status}, timeout=5)
        if response.status_code == 200:
            print(f">>> Successfully updated Order #{order_id} to '{new_status}'")
            return True
        else:
            print(f"[API Error] Failed to update: {response.status_code}")
    except Exception as e:
        print(f"[Connection Error] -> {e}")
    return False

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def main():
    print("Welcome to Python KDS Tester!")
    print("Press Enter to fetch orders. Type 'quit' to exit.")
    
    while True:
        orders = get_orders()
        
        print("\n========================================================")
        print("                 KITCHEN DISPLAY SYSTEM                 ")
        print("========================================================")
        
        if not orders:
            print("\n  [ No Active Orders ]")
        else:
            # Separate by status
            pending = [o for o in orders if o['status'] == 'pending']
            cooking = [o for o in orders if o['status'] == 'cooking']
            
            print(f"\n[NEW ORDERS: {len(pending)}]")
            for o in pending:
                print(f"  Order #{o['id']} - Table {o['table_number']} (Time: {o['created_at'][11:19]})")
                for item in o['items']:
                    notes = f" ({item['notes']})" if item['notes'] else ""
                    print(f"    - {item['quantity']}x {item['menu_item_name']}{notes}")

            print(f"\n[COOKING: {len(cooking)}]")
            for o in cooking:
                print(f"  Order #{o['id']} - Table {o['table_number']} (Time: {o['created_at'][11:19]})")
                for item in o['items']:
                    notes = f" ({item['notes']})" if item['notes'] else ""
                    print(f"    - {item['quantity']}x {item['menu_item_name']}{notes}")
                    
        print("\n--------------------------------------------------------")
        print("Commands: ")
        print("  - Type 'refresh' (or press Enter) to reload")
        print("  - Type '[Order ID] cooking' (e.g. '15 cooking') to start cooking")
        print("  - Type '[Order ID] ready' (e.g. '15 ready') to finish cooking")
        print("  - Type 'quit' to exit")
        print("--------------------------------------------------------")
        
        cmd = input("Command> ").strip().lower()
        
        if cmd == 'quit':
            break
        elif cmd == 'refresh' or cmd == '':
            clear_screen()
            continue
        elif ' ' in cmd:
            parts = cmd.split(' ')
            if len(parts) == 2 and parts[0].isdigit():
                update_status(int(parts[0]), parts[1].strip())
                time.sleep(1) # wait briefly before reloading
                clear_screen()
            else:
                print("Invalid format. Try '15 cooking'")
        else:
            print("Unknown command.")

if __name__ == "__main__":
    main()
