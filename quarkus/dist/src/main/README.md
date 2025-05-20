Keycloak
========

To understand the contents of your Keycloak installation, see the [directory structure guide](https://www.keycloak.org/server/directory-structure).

To get help configuring Keycloak via the CLI, run:

on Linux/Unix:

    $ bin/kc.sh

on Windows:

    $ bin\kc.bat

To try Keycloak out in development mode, run: 

on Linux/Unix:

    $ bin/kc.sh start-dev

on Windows:

    $ bin\kc.bat start-dev

After the server boots, open http://localhost:8080 in your web browser. The welcome page will indicate that the server is running.

To get started, check out the [configuration guides](https://www.keycloak.org/guides#server).

## Keycloak as a Windows Service

This guide explains how to install and run Keycloak as a Windows service using Apache Commons Daemon (Procrun).

### Prerequisites

1. A working Keycloak installation
2. Apache Commons Daemon Procrun executable (`prunsrv.exe`) which can be downloaded from [Apache Commons Daemon](https://downloads.apache.org/commons/daemon/binaries/windows/)

### Setup Instructions

1. Download the appropriate Procrun executable for your architecture:
   - For 64-bit systems: `prunsrv.exe` (amd64)
   - For 32-bit systems: `prunsrv.exe` (x86)

2. Copy `prunsrv.exe` to this location in your Keycloak installation:
   - `<KEYCLOAK_HOME>\bin\prunsrv.exe`

### Installing as a Service

Use the provided script to install Keycloak as a Windows service:

```bash
./service-install.bat [options]
```

#### Options

- `--name <service-name>`: The name of the Windows service (default: keycloak)
- `--display-name <display-name>`: The display name of the service (default: Keycloak Server)
- `--description <description>`: The service description (default: Keycloak Identity and Access Management)
- `--startup <mode>`: Service startup mode: auto, manual (default: auto)
- `--jvm <path>`: Path to jvm.dll to use (default: auto-detected from JAVA_HOME)
- `--keycloak-args "<args>"`: Arguments to pass to Keycloak (e.g., "start-dev --http-port=8080")
- `--jvm-args "<args>"`: Arguments to pass to the JVM (e.g., "-Xms512m -Xmx1024m")
- `--service-user "<username>"`: The user account the service should run as
- `--service-password "<password>"`: The password for the service user account
- `--classpath-jar "<path>"`: Path to quarkus-run.jar if auto-detection fails

#### Service Account Configuration

**Important:** By default, the service is configured to run as the **Local System account**, which has the necessary privileges to run Keycloak successfully.

You have these options:

1. **Run as Local System Account (default and recommended):**
   - Simply omit the `--service-user` and `--service-password` parameters
   - This account has full access to the local system and is the recommended choice
   - Example: `./service-install.bat --name keycloak --keycloak-args "start --http-port=8080"`

2. **Run as Specific User (advanced):**
   - Specify both `--service-user` and `--service-password` parameters
   - The user must have "Log on as a service" privilege
   - Example: `./service-install.bat --service-user "Domain\Username" --service-password "password"`

**Note:** If you experience "Access Denied" errors, ensure the service is running as Local System account, which is the default when no user is specified.

**Troubleshooting:** The service runs under Apache Commons Daemon Service Runner. If you need to forcefully terminate the process, look for `prunsrv.exe` in Task Manager or use `taskkill /f /im prunsrv.exe` (use with caution as this will kill all services using Procrun).

#### Example

```bash
./service-install.bat --name keycloak --keycloak-args "start --http-port=8080" --jvm-args "-Xms512m -Xmx1024m"
```

For development mode with absolute path to quarkus-run.jar:

```bash
./service-install.bat --classpath-jar "C:\path\to\keycloak\quarkus\server\target\lib\quarkus-run.jar" --keycloak-args "start-dev"
```

For production mode with absolute path to quarkus-run.jar:

```bash
./service-install.bat --classpath-jar "C:\path\to\keycloak\quarkus\server\target\lib\quarkus-run.jar" --keycloak-args "start --http-port=8080"
```

To run the service as a specific user (ensure the user has "Log on as a service" privilege):

```bash
./service-install.bat --service-user "Domain\Username" --service-password "password"
```

### Managing the Service

Once installed, you can manage the service using standard Windows tools:

- Start the service: `net start keycloak` (or use the Windows Services console)
  **IMPORTANT**: You must run this command from an Administrator command prompt. To open one:
  1. Click Start, search for "Command Prompt" or "cmd"
  2. Right-click on Command Prompt and select "Run as administrator"
  3. Click Yes on the UAC prompt
  
- Stop the service: `net stop keycloak` (or use the Windows Services console)
- Check service status: Use the Windows Services console

### Uninstalling the Service

To remove the service:

```bash
./service-uninstall.bat [--name <service-name>]
```

### Logs

Service logs are stored in the `log` directory of your Keycloak installation.
