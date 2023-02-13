Example Themes
==============

Deploy Themes
-------------

You can either deploy the themes by copying to the themes folder or as modules.

### Copy

Simplest way to deploy the themes is to copy `src/main/resources/theme/*` to `themes/`.

### Module

Alternatively you can deploy as modules. This can be done by first running:

    mvn clean install
    $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.example.themes --resources=target/keycloak-example-themes.jar"

Then open `standalone/configuration/standalone.xml` and register the theme module by adding:

    <theme>
        ...
        <modules>
            <module>org.keycloak.example.themes</module>
        </modules>
    </theme>

Address Theme
-------------------

Example theme that adds address fields to registration page, account management and admin console. To enable the theme open the admin console, select your realm, click on `Theme`. In the dropdown for `Login Theme` and `Account Theme` select `address`. Click `Save` and login to the realm to see the new theme in action.

One thing to note is that to change the admin console for the master admin console (`/auth/admin`) you need to change the theme for the master realm. Changing the admin console theme for any other realms will only change the admin console for that specific realm (for example `/auth/admin/myrealm/console`).


Sunrise Login Theme
-------------------

Example login theme that changes the look of the login forms. To enable the theme open the admin console, select your realm, click on `Theme`. In the dropdown for `Login Theme` select `sunrise`. Click `Save` and login to the realm to see the new theme in action.


Change Logo Theme
-----------------

To enable the theme open the admin console, select your realm, click on `Theme`. In the dropdowns for `Login Theme`, `Account Theme` and `Admin Console Theme` select `logo-example`. Click `Save` and login to the realm to see the new theme in action.

To change the theme for the welcome pages open `standalone/configuration/standalone.xml` find the config for `theme` and add 'welcomeTheme':

    <theme>
        ...
        <welcomeTheme>logo-example</welcomeTheme>
    </theme>

One thing to note is that to change the admin console for the master admin console (`/auth/admin`) you need to change the theme for the master realm. Changing the admin console theme for any other realms will only change the admin console for that specific realm (for example `/auth/admin/myrealm/console`).
