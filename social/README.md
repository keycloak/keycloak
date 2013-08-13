Keycloak social
===============

This document describes how to configure social providers for Keycloak. At the moment social providers are configured globally using system properties. These can either be passed using '-D' when starting the application server or added to the standalone.xml file, for example:

    <system-properties>
        <property name="keycloak.social.facebook.key" value="<facebook key>"/>
        <property name="keycloak.social.facebook.secret" value="<facebook secret>"/>
        <property name="keycloak.social.google.key" value="<google key>"/>
        <property name="keycloak.social.google.secret" value="<google secret>"/>
        <property name="keycloak.social.twitter.key" value="<twitter key>"/>
        <property name="keycloak.social.twitter.secret" value="<twitter secret>"/>
    </system-properties>

Social provides implementations for Facebook, Google and Twitter.


Configure Facebook
------------------




Configure Google
----------------

Open https://code.google.com/apis/console/. From the drop-down menu select Create.

Use any name that you'd like, click Create Project, select API Access and click on Create an OAuth 2.0 client ID.

Use any product name you'd like and leave the other fields empty, then click Next. On the next page select Web application as the application type. Click more options next> to Your site or hostname. Fill in the form with the following values:

* Authorized Redirect URIs: http://<HOSTNAME>[<PORT>]/auth-server/rest/realms/<REALM>/social/callback

Click on Create client ID. Use the value of Client ID as the value of the system property "keycloak.social.google.key", and the value of Client secret as the value of "keycloak.social.google.secret".


Configure Twitter
-----------------

Open https://dev.twitter.com/apps. Click on Create a new application.

Fill in name, description and website. For Callback URL use the following value:

Callback URL: http://mbaas-stianst.rhcloud.com/ejs-identity/api/callback/516131bc-e3e4-4736-928b-cf1df8c0fe74
Note: Twitter doesn't allow localhost as domain, use 127.0.0.1 instead!

Agree to the rules, fill in the captcha and click on Create your Twitter application.

Now click on Settings and tick the box Allow this application to be used to Sign in with Twitter, and click on Update this Twitter application's settings.

Finally click on Details. Use the value of Client key as the value of the system property "keycloak.social.twitter.key", and the value of Client secret as the value of "keycloak.social.twitter.secret".
