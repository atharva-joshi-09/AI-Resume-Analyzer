@echo off
echo ==============================================
echo   Starting AI Resume Analyzer
echo ==============================================
echo.

if not exist "target\resume-analyzer-1.0.0.jar" (
    echo Building the application first...
    call mvn clean package -DskipTests
)

echo Starting server on http://localhost:8080 ...
echo Press Ctrl+C to stop.
echo.
java -jar target\resume-analyzer-1.0.0.jar
pause
