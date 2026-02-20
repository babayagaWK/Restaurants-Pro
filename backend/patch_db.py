import sqlite3
import os

db_path = 'remote_db.sqlite3'

conn = sqlite3.connect(db_path)
cur = conn.cursor()

try:
    cur.execute("ALTER TABLE core_category ADD COLUMN is_active bool DEFAULT 1 NOT NULL;")
    print("Added is_active to core_category")
except Exception as e:
    print("Error adding is_active:", e)

try:
    cur.execute("ALTER TABLE core_menuitem ADD COLUMN image varchar(100) DEFAULT '';")
    print("Added image to core_menuitem")
except Exception as e:
    print("Error adding image:", e)

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
