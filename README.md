# Keycloak Admin Console

### Build / Run

1. Start keycloak 
    * from source:
        ```bash
        $> cd <keycloak src directory>
        $> cd testsuite/utils
        $> mvn exec:java -Dexec.mainClass="org.keycloak.testsuite.KeycloakServer" -Dkeycloak.port=8180
        ```
    * Download and run
        ```bash
        $> ./start.js
        ```
1. Add the client
    * like this:
    ![realm settings](./realm-settings.png "Realm Settings")
    * or import `security-admin-console-v2.json`

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