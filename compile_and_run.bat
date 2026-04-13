@echo off
title Rock Paper Scissors - Java Game Launcher
color 0B
echo.
echo  ============================================
echo   ROCK  .  PAPER  .  SCISSORS  ^| Java Game
echo  ============================================
echo.

REM ── Try javac from PATH first ──────────────────────────────────────────────
where javac >nul 2>&1
if %errorlevel%==0 (
    set JAVAC=javac
    set JAVA=javaw
    set JAR=jar
    goto :compile
)

REM ── Search common JDK install locations ────────────────────────────────────
for /d %%D in (
    "C:\Program Files\Java\jdk*"
    "C:\Program Files\Eclipse Adoptium\jdk*"
    "C:\Program Files\Microsoft\jdk*"
    "C:\Program Files\BellSoft\LibericaJDK*"
    "C:\Program Files\Amazon Corretto\jdk*"
    "%LOCALAPPDATA%\Programs\Java\jdk*"
) do (
    if exist "%%D\bin\javac.exe" (
        set JAVAC=%%D\bin\javac.exe
        set JAVA=%%D\bin\javaw.exe
        set JAR=%%D\bin\jar.exe
        goto :compile
    )
)

REM ── JDK not found ──────────────────────────────────────────────────────────
echo  [ERROR] Java JDK not found!
echo.
echo  Please install Java JDK 17 or later from one of these sources:
echo    - https://adoptium.net           (Temurin / Eclipse Adoptium - Recommended)
echo    - https://www.oracle.com/java/   (Oracle JDK)
echo    - https://corretto.aws           (Amazon Corretto)
echo.
echo  After installing, rerun this script.
echo.
pause
exit /b 1

:compile
echo  [OK] Found Java compiler:
echo       %JAVAC%
echo.
echo  Compiling Ultimate Edition source files...
"%JAVAC%" *.java
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo.
echo  Building Executable JAR (RockPaperScissors.jar)...
"%JAVAC%" -d . *.java
"%JAR%" cvfe RockPaperScissors.jar RockPaperScissors *.class *.png *.jpg >nul

echo.
echo  [SUCCESS] Launching your game!
echo.

start "" "%JAVA%" -jar RockPaperScissors.jar
exit /b 0
