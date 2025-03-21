@echo off

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

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

"%JAVA%" %KC_OPTS% -cp "%DIRNAME%\client\keycloak-admin-cli-${project.version}.jar" --add-opens=java.base/java.security=ALL-UNNAMED -Dkc.lib.dir="%DIRNAME%\client\lib" org.keycloak.client.registration.cli.KcRegMain %*

:END
