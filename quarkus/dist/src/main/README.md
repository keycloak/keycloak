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

This guide explains how to install and run Keycloak as a Windows service using [Apache Commons Daemon (Procrun)](https://commons.apache.org/proper/commons-daemon/procrun.html).

### How It Works

The Windows service uses Procrun in **exe mode**, which means:
- The service directly executes `kc.bat start` as an external process
- All environment variables (like `JAVA_HOME`, `KC_*` variables) are respected
- Configuration from `conf/keycloak.conf` is used
- The service behaves exactly like running `kc.bat start` manually
- The `kc.bat` script handles automatic re-augmentation if needed

This approach ensures full compatibility with all Keycloak configuration options and environment variable handling.

### Prerequisites

1. A working Keycloak installation
2. Apache Commons Daemon Procrun executable (`prunsrv.exe`) which can be downloaded from [Apache Commons Daemon](https://downloads.apache.org/commons/daemon/binaries/windows/)

### Setup Instructions

1. Download the appropriate Procrun executable for your architecture:
   - For 64-bit systems: `prunsrv.exe` (amd64)
   - For 32-bit systems: `prunsrv.exe` (x86)

2. Copy `prunsrv.exe` to this location in your Keycloak installation:
   - `<KEYCLOAK_HOME>\bin\prunsrv.exe`

### Optional: Pre-build

```bash
bin\kc.bat build [your-build-options]
```

For example, to build with PostgreSQL database support:

```bash
bin\kc.bat build --db=postgres
```

**Note:** Pre-building is optional. If you skip this step, Keycloak will automatically build on first start, which takes longer but works correctly.

### Creating the Service

Use the following command to create Keycloak as a Windows service:

```bash
bin\kc.bat service create [options]
```

To see all available options:

```bash
bin\kc.bat service create --help
```


#### Service Dependencies

Use `--depends-on` to ensure other services start before Keycloak. This is useful when Keycloak depends on a database service:

```bash
bin\kc.bat service create --depends-on="postgresql-x64-15"
```

For multiple dependencies, separate them with semicolons:

```bash
bin\kc.bat service create --depends-on="postgresql-x64-15;Tcpip;Afd"
```

**Note:** By default, Procrun adds dependencies on `Tcpip` and `Afd` network services during installation.

#### Service Account Configuration

**Important:** By default, the service is configured to run as the **Local System account**, which has the necessary privileges to run Keycloak successfully.

You have these options:

1. **Run as Local System Account (default and recommended):**
   - Simply omit the `--service-user` and `--service-password` parameters
   - This account has full access to the local system and is the recommended choice
   - Example: `bin\kc.bat service create --name keycloak`

2. **Run as Specific User (advanced):**
   - Specify both `--service-user` and `--service-password` parameters
   - The user must have "Log on as a service" privilege
   - Example: `bin\kc.bat service create --service-user="Domain\Username" --service-password="password"`

**Note:** If you experience "Access Denied" errors, ensure the service is running as Local System account, which is the default when no user is specified.

#### Examples

Create a basic service:

```bash
bin\kc.bat service create --name keycloak
```

Create with a database dependency:

```bash
bin\kc.bat service create --name keycloak --depends-on="postgresql-x64-15"
```

Create with manual startup mode and longer stop timeout:

```bash
bin\kc.bat service create --startup=manual --stop-timeout=60
```

Create with delayed auto-start (starts after other auto-start services):

```bash
bin\kc.bat service create --startup=delayed
```

Create with a custom service name:

```bash
bin\kc.bat service create --name=my-keycloak --display-name="My Keycloak Server"
```

Create to run as a specific user (ensure the user has "Log on as a service" privilege):

```bash
bin\kc.bat service create --service-user="Domain\Username" --service-password="password"
```

### Managing the Service

Once created, you can manage the service using standard Windows tools:

- **Start the service:** `net start keycloak` (or use the Windows Services console)
  
  **IMPORTANT**: You must run this command from an Administrator command prompt. To open one:
  1. Click Start, search for "Command Prompt" or "cmd"
  2. Right-click on Command Prompt and select "Run as administrator"
  3. Click Yes on the UAC prompt
  
- **Stop the service:** `net stop keycloak` (or use the Windows Services console)
- **Check service status:** Use the Windows Services console (`services.msc`)

### Logging

Since the service runs in exe mode, logging works through Keycloak's built-in logging system.

#### Enabling File Logging (Recommended for Services)

Add the following to your `conf/keycloak.conf` to enable file logging:

```properties
# Enable file logging
log=file

# Configure log file location (default: <KEYCLOAK_HOME>/data/log/keycloak.log)
log-file=${kc.home.dir}/log/keycloak.log

# Optional: Configure log format
log-file-format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n

# Optional: Configure log level
log-level=INFO
```

#### Log File Locations

- **Keycloak application logs:** Configured via `log-file` in `keycloak.conf` (recommended: `<KEYCLOAK_HOME>/log/keycloak.log`)
- **Procrun service wrapper logs:** `<KEYCLOAK_HOME>/log/` directory (files named `commons-daemon.YYYY-MM-DD.log`)

### Configuration Changes

If you need to change the Keycloak configuration:

1. Stop the service: `net stop keycloak`
2. Optionally re-run the build: `bin\kc.bat build [new-options]`
3. Update `conf/keycloak.conf` if needed for runtime options
4. Start the service: `net start keycloak`

Runtime configuration options can be set via:
- Environment variables (set in System Properties)
- The `conf/keycloak.conf` configuration file

### Deleting the Service

To remove the service:

```bash
bin\kc.bat service delete [--name <service-name>]
```

**Note:** If the service is running, stop it first using `net stop keycloak`.

### Troubleshooting

#### Service fails to start
1. Check the log files in the `log` directory
2. Try running `bin\kc.bat start` manually to see any errors
3. Verify `conf/keycloak.conf` has correct runtime options

#### Access Denied errors
- Ensure the service is running as Local System account (default)
- If using a specific user, ensure they have "Log on as a service" privilege

#### Forcefully terminate the service
The service runs under Apache Commons Daemon Service Runner. If you need to forcefully terminate:
- Look for `prunsrv.exe` in Task Manager
- Or use: `taskkill /f /im prunsrv.exe` (use with caution as this affects all Procrun services)

### Procrun Configuration

The service uses [Apache Commons Daemon Procrun](https://commons.apache.org/proper/commons-daemon/procrun.html) with the following configuration:

- **StartMode:** exe (runs kc.bat as external process)
- **StartImage:** `<KEYCLOAK_HOME>\bin\kc.bat`
- **StartParams:** `start`
- **StopMode:** exe
- **StopImage:** `<KEYCLOAK_HOME>\bin\kc.bat`  
- **StopParams:** `stop`
- **StopTimeout:** configurable (default: 30 seconds)

Service configuration is stored in the Windows Registry under:
`HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Apache Software Foundation\ProcRun 2.0\<ServiceName>`
