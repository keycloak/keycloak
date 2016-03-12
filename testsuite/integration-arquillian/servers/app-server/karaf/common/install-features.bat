set NOPAUSE=true
echo "JAVA_HOME=%JAVA_HOME%"

start.bat
echo "Karaf container starting"
timout /t 5

set ERROR=0
set TIMEOUT=10
set I=0

:wait_for_karaf
call client.bat %CLIENT_AUTH% info
if %ERRORLEVEL% equ 0 goto install_features
echo "Server is not reachable. Waiting."
timeout /t 1
set /a I=%I%+1
if %I% gtr %TIMEOUT% (
    set ERROR=1
    goto shutdown_karaf
)
goto wait_for_karaf


:install_features
echo "Server is reachable. Installing features."
call client.bat %CLIENT_AUTH% -f install-features.cli
if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%


:shutdown_karaf
call stop.bat
timeout /t 5
exit /b %ERROR%
