@echo off
echo ==========================================
echo   Building Test Functions
echo ==========================================

where mvn >nul 2>nul || (echo ERROR: Maven ist nicht installiert! && exit /b 1)

echo Baue alle Test-Functions...
call mvn clean package -q

echo Kopiere JARs nach jars\...
if not exist jars mkdir jars
copy helloF\target\hello-function.jar jars\
copy reverseF\target\reverse-function.jar jars\
copy sumF\target\sum-function.jar jars\

echo.
echo ==========================================
echo   Build erfolgreich!
echo ==========================================
echo.
echo Erstellte JARs:
dir jars\*.jar
