@echo off
echo Initializing Git repository...
git init

echo Adding files...
git add .

echo Committing...
git commit -m "Initial commit with Android project scaffold"

echo Renaming branch to main...
git branch -M main

echo Adding remote...
git remote add origin https://github.com/babayagaWK/Restaurants-Pro.git

echo.
echo =======================================================
echo Local Git repository has been set up successfully!
echo.
echo Now, to push the code to GitHub and trigger the KDS build,
echo please run the following command in your terminal:
echo =======================================================
echo.
echo git push -u origin main
echo.
echo (You may be prompted to log in to GitHub with your browser)
echo =======================================================
pause
