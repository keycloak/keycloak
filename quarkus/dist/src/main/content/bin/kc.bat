@echo off
rem -------------------------------------------------------------------------
rem Keycloak Startup Script
rem -------------------------------------------------------------------------

@if not "%ECHO%" == ""  echo %ECHO%
setlocal

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set PROGNAME=%~nx0%
) else (
  set PROGNAME=kc.bat
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0"
) else (
  set DIRNAME=.\
)

set SERVER_OPTS=-Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dquarkus-log-max-startup-records=10000 -Dpicocli.disable.closures=true

set DEBUG_MODE=false
set DEBUG_PORT_VAR=8787
set DEBUG_SUSPEND_VAR=n
set CONFIG_ARGS=

rem Read command-line args, the ~ removes the quotes from the parameter
:READ-ARGS
set "KEY=%~1"
if "%KEY%" == "" (
    goto MAIN
)
if "%KEY%" == "--debug" (
  set DEBUG_MODE=true
  if 1%2 EQU +1%2 (
     set DEBUG_PORT_VAR=%2
     shift
  ) else (
     set DEBUG_PORT_VAR=8787
  )
  shift
  goto READ-ARGS
)
if "%KEY%" == "start-dev" (
  set CONFIG_ARGS=%CONFIG_ARGS% --profile=dev %KEY%
  shift
  goto READ-ARGS
)
set "VALUE=%~2"
set PROBABLY_VALUE=false
if "%VALUE%" NEQ "" (
    if "%VALUE:~0,1%" NEQ "-" (
        if "%KEY:^==%"=="%KEY%" (
            set PROBABLY_VALUE=true
        )
    )
)
if "%KEY:~0,2%"=="-D" (
  if %PROBABLY_VALUE%==true (
    set SERVER_OPTS=%SERVER_OPTS% %KEY%^=%VALUE%
    shift
  ) else (
    set SERVER_OPTS=%SERVER_OPTS% %KEY%
  )
  shift
  goto READ-ARGS
)
if not "%KEY:~0,1%"=="-" (
  set CONFIG_ARGS=%CONFIG_ARGS% %1
  shift
  goto READ-ARGS
)
if %PROBABLY_VALUE%==true (
  set CONFIG_ARGS=%CONFIG_ARGS% %1 %2
  shift
) else (
  set CONFIG_ARGS=%CONFIG_ARGS% %1
)
shift
goto READ-ARGS

:MAIN

setlocal EnableDelayedExpansion

if not "x%JAVA_OPTS%" == "x" (
  echo "JAVA_OPTS already set in environment; overriding default settings"
) else (
  rem The defaults set up Keycloak with '-XX:+UseG1GC -XX:MinHeapFreeRatio=40 -XX:MaxHeapFreeRatio=70 -XX:GCTimeRatio=12 -XX:AdaptiveSizePolicyWeight=10' which proved to provide a good throughput and efficiency in the total memory allocation and CPU overhead.
  rem If the memory is not used, it will be freed. See https://developers.redhat.com/blog/2017/04/04/openjdk-and-containers for details.
  rem To optimize for large heap sizes or for throughput and better response time due to shorter GC pauses, consider ZGC and Shenandoah GC.
  rem As of KC25 and JDK17, G1GC, ZGC and Shenandoah GC seem to be eager to claim the maximum heap size. Tests showed that ZGC might need additional tuning in reclaiming dead objects.
  set "JAVA_OPTS=-XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.err.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom -XX:+UseG1GC -XX:FlightRecorderOptions=stackdepth=512 -Djdk.tls.rejectClientInitiatedRenegotiation=true"

  if "x%JAVA_OPTS_KC_HEAP%" == "x" (
    if "!KC_RUN_IN_CONTAINER!" == "true" (
      rem Maximum utilization of the heap is set to 70% of the total container memory
      rem Initial heap size is set to 50% of the total container memory in order to reduce GC executions
      set "JAVA_OPTS_KC_HEAP=-XX:MaxRAMPercentage=70 -XX:MinRAMPercentage=70 -XX:InitialRAMPercentage=50"
    ) else (
      set "JAVA_OPTS_KC_HEAP=-Xms64m -Xmx512m"
    )

  ) else (
    echo "JAVA_OPTS_KC_HEAP already set in environment; overriding default settings"
  )

  set "JAVA_OPTS=!JAVA_OPTS! !JAVA_OPTS_KC_HEAP!"
)

@REM See also https://github.com/wildfly/wildfly-core/blob/7e5624cf92ebe4b64a4793a8c0b2a340c0d6d363/core-feature-pack/common/src/main/resources/content/bin/common.sh#L57-L60
if not "x%JAVA_ADD_OPENS%" == "x" (
  echo "JAVA_ADD_OPENS already set in environment; overriding default settings"
) else (
  set "JAVA_ADD_OPENS=--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED"
)
set "JAVA_OPTS=%JAVA_OPTS% %JAVA_ADD_OPENS%"

@REM Set the default locale for the JVM to English to prevent locale-specific character variations
if not "x%JAVA_LOCALE%" == "x" (
  echo "JAVA_LOCALE already set in environment; overriding default settings"
) else (
  set "JAVA_LOCALE=-Duser.language=en -Duser.country=US"
)
set "JAVA_OPTS=%JAVA_OPTS% %JAVA_LOCALE%"

if not "x%JAVA_OPTS_APPEND%" == "x" (
  echo "Appending additional Java properties to JAVA_OPTS"
  set JAVA_OPTS=%JAVA_OPTS% %JAVA_OPTS_APPEND%
)

if NOT "x%DEBUG%" == "x" (
  set DEBUG_MODE=%DEBUG%
)

if NOT "x%DEBUG_PORT%" == "x" (
  set DEBUG_PORT_VAR=%DEBUG_PORT%
)

if NOT "x%DEBUG_SUSPEND%" == "x" (
  set DEBUG_SUSPEND_VAR=%DEBUG_SUSPEND%
)

rem Set debug settings if not already set
if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT_VAR%,server=y,suspend=%DEBUG_SUSPEND_VAR%
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

rem Setup Keycloak specific properties
set JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%

if "x%JAVA%" == "x" (
  if "x%JAVA_HOME%" == "x" (
    set JAVA=java
    echo JAVA_HOME is not set. Unexpected results may occur. 1>&2
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message. 1>&2
  ) else (
    if not exist "%JAVA_HOME%" (
      echo JAVA_HOME "%JAVA_HOME%" path doesn't exist 1>&2
      goto END
    ) else (
      if not exist "%JAVA_HOME%\bin\java.exe" (
        echo "%JAVA_HOME%\bin\java.exe" does not exist 1>&2
        goto END
      )
      set "JAVA=%JAVA_HOME%\bin\java"
    )
  )
)

set CLASSPATH_OPTS="%DIRNAME%..\lib\quarkus-run.jar"

rem set the homedir with \ replaced by /
set KC_HOME_DIR=%DIRNAME%..
set KC_HOME_DIR=%KC_HOME_DIR:\=/%

rem The property 'java.util.concurrent.ForkJoinPool.common.threadFactory' is set here, as a Java Agent or enabling JMX might initialize the factory before Quarkus can set the property in JDK21+.
set JAVA_RUN_OPTS=-Djava.util.concurrent.ForkJoinPool.common.threadFactory=io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory %JAVA_OPTS% -Dkc.home.dir="%KC_HOME_DIR%" -Djboss.server.config.dir="%DIRNAME%..\conf" -Dkeycloak.theme.dir="%DIRNAME%..\themes" %SERVER_OPTS% -cp %CLASSPATH_OPTS% io.quarkus.bootstrap.runner.QuarkusEntryPoint %CONFIG_ARGS%

set OPTIMIZED_OPTION=--optimized
set HELP_LONG_OPTION=--help
set BUILD_OPTION=build
set IS_HELP_SHORT=false

echo %CONFIG_ARGS% | findstr /r "\<-h\>" > nul

if not errorlevel == 1 (
    set IS_HELP_SHORT=true
)

if "%PRINT_ENV%" == "true" (
  echo Using JAVA_OPTS: !JAVA_OPTS!
  echo Using JAVA_RUN_OPTS: !JAVA_RUN_OPTS!
)

set START_SERVER=true

if "!CONFIG_ARGS:%OPTIMIZED_OPTION%=!"=="!CONFIG_ARGS!" if "!CONFIG_ARGS:%BUILD_OPTION%=!"=="!CONFIG_ARGS!" if "!CONFIG_ARGS:%HELP_LONG_OPTION%=!"=="!CONFIG_ARGS!" if "%IS_HELP_SHORT%" == "false" (

    "%JAVA%" -Dkc.config.build-and-exit=true !JAVA_RUN_OPTS!

    if not !errorlevel! == 0 (
        set START_SERVER=false
    )

    set JAVA_RUN_OPTS=-Dkc.config.built=true !JAVA_RUN_OPTS!
)

if "%START_SERVER%" == "true" (
    "%JAVA%" !JAVA_RUN_OPTS!
)

:END
