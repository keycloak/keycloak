set NOPAUSE=true
echo "JAVA_HOME=%JAVA_HOME%"

start "Fuse" /b cmd /c start.bat
echo "Fuse container starting"
ping 127.0.0.1 -n 5 > nul

set ERROR=0
set TIMEOUT=10
set I=0

:wait_for_fuse
call client.bat -u admin -p admin info
if %ERRORLEVEL% equ 0 goto install_features
echo "Server is not reachable. Waiting."
ping 127.0.0.1 -n 2 > nul
set /a I=%I%+1
if %I% gtr %TIMEOUT% (
    set ERROR=1
    goto shutdown_fuse
)
goto wait_for_fuse


:install_features
echo "Server is reachable. Installing features."
if "%UNINSTALL_PAX%" == "true" (
    call client.bat -u admin -p admin -f uninstall-pax.cli
    if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
)
if "%UPDATE_CONFIG%" == "true" (
    call client.bat -u admin -p admin -f update-config.cli
    if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
)
call client.bat -u admin -p admin -f install-features.cli
if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%


:shutdown_fuse
call stop.bat
ping 127.0.0.1 -n 5 > nul
exit /b %ERROR%
