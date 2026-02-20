import sqlite3
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
