@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
    echo ERREUR : JDK 21 introuvable. Verifiez java -version et javac -version.
    pause
    exit /b 1
)
if not exist "build\classes" mkdir "build\classes"
if not exist "dist" mkdir "dist"
javac --release 21 -encoding UTF-8 -d "build\classes" "src\main\java\com\ballgame\pro\BallGame.java" "src\main\java\com\ballgame\pro\AudioEngine.java" "src\main\java\com\ballgame\pro\ScoreStore.java" "src\main\java\com\ballgame\pro\AudioGenerator.java"
if errorlevel 1 (pause & exit /b 1)
xcopy "src\main\resources\*" "build\classes\" /E /I /Y >nul
(
    echo Manifest-Version: 1.0
    echo Main-Class: com.ballgame.pro.BallGame
    echo.
) > "build\manifest.mf"
jar --create --file "dist\BallGame-Pro-V2.jar" --manifest "build\manifest.mf" -C "build\classes" .
if errorlevel 1 (pause & exit /b 1)
echo JAR cree : dist\BallGame-Pro-V2.jar
endlocal
