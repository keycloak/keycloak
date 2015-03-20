Example Themes
==============

Sunrise Login Theme
-------------------

Example login theme that changes the look of the login forms.

To use the theme you can either deploy it as a module or copy it to the Keycloaks themes folder.

To deploy as a module run:

    mvn clean install
    $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.example.sunrisetheme --resources=target/keycloak-example-themes.jar"

Then open $KEYCLOAK_HOME/standalone/configuration/keycloak-server.json and register the theme module by adding:
    
    "theme": {
        "module": {
            "modules": [ "org.keycloak.example.sunrisetheme" ]
        }
    }

Alternatively you can copy `src/main/resources/theme/login` to `standalone/configuration/themes/login/`. 

Once you've added the theme open the admin console, select your realm, click on `Theme`. In the dropdown for `Login Theme` select `sunrise`. Click `Save` and login to the realm to see the new theme in action.


Change Logo Theme
-----------------

Example themes for login forms, account management, admin console and welcome pages that changes the Keycloak logo.

To use the themes you can either deploy it as a module or copy it to the Keycloaks themes folder.

To deploy as a module run:

   mvn clean install
   $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.example.logotheme --resources=target/keycloak-example-themes.jar"

Then open $KEYCLOAK_HOME/standalone/configuration/keycloak-server.json and register the theme module by adding:
    
    "theme": {
        "module": {
            "modules": [ "org.keycloak.example.logotheme" ]
        }
    }

Alternatively you can copy:

* `account/logo-example` to `standalone/configuration/themes/account/`
* `login/logo-example` to `standalone/configuration/themes/login/`
* `admin/logo-example` to `standalone/configuration/themes/admin/`
* `welcome/logo-example` to `standalone/configuration/themes/welcome/`


Once you've added the theme open the admin console, select your realm, click on `Theme`. In the dropdowns for `Login Theme`, `Account Theme` and `Admin Console Theme` select `logo-example`. Click `Save` and login to the realm to see the new theme in action.

To change the theme for the welcome pages open `standalone/configuration/keycloak-server.json` find the config for `theme` and add 'welcomeTheme':

    "theme": {
        ...
        "welcomeTheme": "logo-example"
    },

One thing to note is that to change the admin console for the master admin console (`/auth/admin`) you need to change the theme for the master realm. Changing the admin console theme for any other realms will only change the admin console for that specific realm (for example `/auth/admin/myrealm/console`).
