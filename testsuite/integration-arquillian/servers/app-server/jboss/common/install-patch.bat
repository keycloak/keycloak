set NOPAUSE=true
setlocal EnableDelayedExpansion

 for %%a in (%APP_PATCH_ZIPS%) do (
   set patch=%%a
   if "!patch:~0,4!"=="http" (
      powershell -command "& { iwr %%a -OutFile %cd%\patch.zip }"
      call %JBOSS_HOME%\bin\jboss-cli.bat --command="patch apply %cd%\patch.zip
   )  else (
      call %JBOSS_HOME%\bin\jboss-cli.bat --command="patch apply %%a"
   )
   if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
 )
 exit /b %ERROR%