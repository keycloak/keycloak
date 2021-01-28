# Keycloak Admin Console V2
This project is the next generation of the Keycloak Administration Console.  It is written with React and [PatternFly 4][1].

### Development Instructions

For development on this project you will need a running Keycloak server listening on port 8180.  You will also need [yarn installed on your local machine.][2]

1. Start keycloak
    * Download and run with one command
        ```bash
        $> ./start.js
        ```
    * or download Keycloak server from [keycloak downloads page][3] unpack and run it like:
        ```bash
        $> cd <unpacked download folder>/bin
        $> standalone -Djboss.socket.binding.port-offset=100
        ```
1. Go to the clients section of the existing Keycloak Admin Console and add the client
    * like this:
    ![realm settings](./realm-settings.png "Realm Settings")
    * or click on the "Select file" button and import `security-admin-console-v2.json`
    * or run `$> ./import.js`

1. Install dependencies and run:
    ```bash
    $> yarn
    $> yarn start
    ```

### Additionally there are some nice scripts to format and lint

```bash
$> yarn format
$> yarn check-types
$> yarn lint
```

To switch to a RH-SSO themed version of this console you can run:

```bash
$> npx grunt switch-rh-sso
```

To switch back just do a `git checkout public`

[1]: https://www.patternfly.org/v4/
[2]: (https://classic.yarnpkg.com)
[3]: https://www.keycloak.org/downloads