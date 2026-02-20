# Restaurant POS & KDS (MVP)

System Architecture for a Restaurant Point of Sale and Kitchen Display System.

## Project Structure
- `backend/`: Django + DRF project (Python).
- `android/`: Kotlin source files for the Android app.

## Quick Setup (Windows)
Double-click `setup.bat` to automatically install dependencies and initialize the database.

## Manual Backend Setup (Django)

1.  **Install Dependencies:**
    ```bash
    cd backend
    pip install -r requirements.txt
    ```

2.  **Initialize Database:**
    ```bash
    python manage.py makemigrations
    python manage.py migrate
    ```

3.  **Create Admin User:**
    ```bash
    python manage.py createsuperuser
    ```

4.  **Run Server:**
    ```bash
    python manage.py runserver
    ```
    - API will be available at `http://127.0.0.1:8000/api/`
    - Admin panel at `http://127.0.0.1:8000/admin/`

## Deployment (PythonAnywhere)
1.  Upload the `backend/` folder to PythonAnywhere.
2.  Set up a Virtualenv and install requirements.
3.  Configure WSGI file to point to `pos_project.settings`.
4.  Run `python manage.py migrate` in the PythonAnywhere console.
5.  **Important:** The `settings.py` is currently set to `ALLOWED_HOSTS = ['*']` and `CORS_ALLOW_ALL_ORIGINS = True` for easy development. Tighten these security settings for production.

## Android Client
This folder contains the Kotlin source files for the Android application.
- `data/model/`: Data classes for JSON parsing.
- `data/api/`: Retrofit service definition.
- `data/repository/`: Data repository with Short Polling logic.

**Key Feature - Polling:**
Since PythonAnywhere's free tier does not support WebSockets, the app uses **Short Polling** (every 10 seconds) in `OrderRepository.kt` to fetch new orders for the Kitchen Display System.

## Roadmap (Future Features)
- [ ] **Authentication:** Add JWT Login for staff.
- [ ] **Real-time Updates:** Migrate to Firebase Cloud Messaging (FCM) for push notifications (removes polling overhead).
- [ ] **Printing:** Integrate with ESC/POS Thermal Printers (via Bluetooth/USB).
- [ ] **Offline Mode:** Use Room Database for offline order syncing.
- [ ] **Analytics:** Dashboard for daily sales and popular items.
