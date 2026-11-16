@echo off
echo ==============================================
echo   Starting AI Resume Analyzer
echo ==============================================
echo.

:: Load .env file if present
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if not "%%A"=="" if not "%%B"=="" set "%%A=%%B"
    )
)

if "%AI_API_KEY%"=="" (
    echo [Demo Mode] AI_API_KEY is not set.
    echo Running in offline Demo Analysis Mode (local rule-based checks).
    echo To enable live Gemini AI analysis, set AI_API_KEY in .env or your environment.
) else (
    echo [AI Mode] Gemini API key detected! Running with live AI model.
)
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
