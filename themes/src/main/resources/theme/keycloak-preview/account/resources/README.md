New Account Console
========

The new Account Console uses PatternFly 4 and React.

Here is what you need to know in order to do development on the new console.


Building
--------
First, you must install node and npm.  Then run install.

`npm install`

To build and lint the application:

`npm run build`

If your IDE doesn't handle dynamic TypeScript transpilation, use the watch script:

`npm run build:watch`

If you want to run lint by itself**:

`npm run lint`

** Please lint the code and fix any errors or warnings before doing a commit.

Edit standalone.xml
--------
Your standalone.xml should have the following set in the keycloak subsystem:

```xml
<theme>
    <staticMaxAge>-1</staticMaxAge>
    <cacheThemes>false</cacheThemes>
    <cacheTemplates>false</cacheTemplates>
    <dir>path_to_keycloak_dev/keycloak/themes/src/main/resources/theme</dir>
</theme>
```

Running
--------
You should run the Keycloak server with the following system properties:

`standalone -Dkeycloak.profile.feature.account_api=enabled -Dkeycloak.profile.feature.account2=enabled`

Log in to the admin console.  Go to Realm Settings --> Themes.  Then set account theme to "keycloak-preview".

Now when you go to account management you should see the new React/PF4 version.