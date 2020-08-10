# Keycloak Admin Console V2
This project is the next generation of the Keycloak Administration Console.  It is written with React and PatternFly 4.

### Development Instructions

For development on this project you will need a running Keycloak server listening on port 8180.  You will also need [yarn installed on your local machine.](https://classic.yarnpkg.com)

1. Start keycloak
    * Download Keycloak server from [https://www.keycloak.org/downloads]()
    * Start Keycloak server like this from the bin directory:
        ```bash
        $> standalone -Djboss.socket.binding.port-offset=100
        ```
    * or download and run with one command
        ```bash
        $> ./start.js
        ```
1. Add the client
    * like this:
    ![realm settings](./realm-settings.png "Realm Settings")
    * or click on the "Select file" button and import `security-admin-console-v2.json`

1. Install dependecies and run:
    ```bash
    $> yarn
    $> yarn start:dev
    ```

### Additionally there are some nice scripts to format and lint

```bash
$> yarn format
$> yarn lint
```