Basic Cordova Example
=====================

Before running this example you need to have Cordova installed with a phone or emulator available.

Start and configure Keycloak
----------------------------

Start Keycloak bound to an IP address available to the phone or emulator. For example:

    bin/standalone.sh -b 192.168.0.10

Open the Keycloak admin console, click on Add Realm, click on 'Choose a JSON file', selct example-realm.json and click Upload.

Navigate to applications, click on 'Cordova', select 'Installation' and in the 'Format option' drop-down select 'keycloak.json'. Download this file to the www folder.

Download '/js/keycloak.js' from the server to the www folder as well. For example:

    wget http://192.168.0.10:8080/auth/js/keycloak.js


Install to Android phone or emulator
------------------------------------

    mkdir platforms plugins
    cordova plugin add cordova-plugin-inappbrowser
    cordova plugin add cordova-plugin-whitelist
    cordova platform add android
    cordova run android


Once the application is opened you can login with username: 'user', and password: 'password'.


Troubleshooting
-----------------------------------------

 * You always need to initialize keycloak after the 'deviceready' event. Otherwise Cordova mode won't be enabled for keycloak.js.
 * 'http://localhost' should be listed in the allowed redirects in client configuration, but never 'file:///android_asset'.
