New Account Console
========

The new Account Console uses PatternFly 4 and React.

Here is what you need to know in order to do development on the new console.


Building
--------
First, you must install node and npm.  The recommended version of npm is 6.9.0 or higher.

Then run install.

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

Errors Using PatternFly 4 Components
--------
If you start using a new PatternFly 4 component or icon and the component is not found, it is probably because it is not activated in `systemjs.config.js`.  To avoid downloading unused components and icons, we declare them as @empty in the config file.

So to fix the problem, edit `system.config.js` and follow the instructions in the file for components and icons.  

Note that you will also need to fix this problem for any components and icons used by the parent component.  Unfortunately, this may require looking at the source code for patternfly-react.  See https://github.com/patternfly/patternfly-react/tree/master/packages/patternfly-4/react-core/src/components

Running tests
-------------
1. Build New Account Console as stated in the "Building" chapter above
1. Build the project (no need to build the whole distribution).
1. Run:
```
mvn clean verify -f testsuite/integration-arquillian/tests/other/base-ui -Dtest=**.account2.** -Dbrowser=chrome -DchromeArguments=--enable-web-authentication-testing-api
```
Use `chrome` or `firefox` as the browser, other browsers are currently broken for the testsuite.

**You need to rebuild the `themes` module prior to running tests every time you make a change to the Account Console**