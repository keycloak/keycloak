Unconfigured Examples
===================================
This set of projects contains a stripped down version of the demo described in preconfigured-demo.  All keycloak specific
configuration has been removed. Use this project in conjunction with instructions below and/or the online screencast tutorials at
[http://keycloak.org/docs](http://keycloak.org/docs).


# Run Demo with an external Keycloak Server

These instructions assume you've already installed and started a Keycloak Server. Keycloak can be running on your locally or remotely (for example on OpenShift). If you're not running Keycloak locally you'll also need a locally running WildFly server.

## Create Realm

Open the Keycloak admin console and click on `Add Realm`. Enter `demo` as the name for the realm and click `Save`.

The demo applications uses two realm roles, `user` and `admin`, so the next step is to create these. Click on `Roles` then click on `Add Role`. Use `user` as the role name and click `Save`. Repeat to create a role with the name `admin`.

Next you'll either want to enable user registration or create a new user.

### Enable user registration

To enable user registration first click on `Roles` then `Default Roles`. Select the `user` role and click on the right arrow. This will make sure that all new users are automatically assigned the `user` role. Next step is to enable user registration for the realm. Click on `Settings` then `Login`. Click on the toggle for `user registration` to allow users to self-register.

### Create user

To create a new user click on `Users` then `Add User`. You are required to at least fill in the `username` field, but you may want to fill in values for the other fields as well. After you've completed the form click on `Save`. To allow the user to login you also need to set a password for the user. To do this click on `Credentials`. Enter a new password for the user. If you leave the `Temporary` toggle ON the user will be required to reset the password on the next login.


## Deploy Demo Applications

First you need to install WildFly application server. Second step is to install the Keycloak WildFly subsystem. To do this run:

    # cd <WILDFLY HOME>
    # unzip <KEYCLOAK DIST>/adapters/keycloak-wildfly-adapter-dist-<KEYCLOAK VERSION>.zip

Next configure the Keycloak adapter by editing `standalone/configuration/standalone.xml`. Add a new child-element to `<extensions>`:

    <extensions>
        ....
        <extension module="org.keycloak.keycloak-subsystem"/>
    </extensions>

You also need to add realm config to the same file. Add a new child-element to `<profile>`:

    <profile>
        ....
        <subsystem xmlns="urn:jboss:domain:keycloak:1.1">
            <realm name="demo">
                <auth-server-url>KEYCLOAK URL</auth-server-url>
                <ssl-required>external</ssl-required>
            </realm>
        </subsystem>
    </profile>

In the above snippet replace the following:

* `KEYCLOAK URL` - replace with the base url of Keycloak (for example http://localhost:8080/auth or http://keycloak.example.org/auth)

Don't start the WildFly server until you've configured and deployed the demo applications.

### Database Services

Most demo applications connects to the REST services provided by the database-services application, so start with deploying this.

Run the following to deploy it:

    # cd database-services
    # mvn install
    # cp target/database.war <WILDFLY HOME>/standalone/deployments

Next add the configuration for it to the Keycloak subsystem. Edit `<WILDFLY HOME>/standalone/configuration/standalone.xml` to `<subsystem xmlns="urn:jboss:domain:keycloak:1.1">` add:

    <secure-deployment name="database.war">
        <realm>demo</realm>
        <resource>database-service</resource>
        <bearer-only>true</bearer-only>
    </secure-deployment>

### Customer Portal

Next deploy the customer portal application.

Run the following to deploy it:

    # cd customer-app
    # mvn install
    # cp target/customer-portal.war <WILDFLY HOME>/standalone/deployments

Then open the Keycloak admin console to add a configuration for it. Navigate to the realm and click on `Clients` then `Add Client`. Fill in the form with:

* Client ID - `customer-portal`

Then click on `Save`. You will see more possibilities to setup client now, so you can add the following:
`Access Type` - `confidential`
`Valid Redirect URIs` - `http://localhost:8080/customer-portal/*` (click `Add` after filling in the field)

Then click on `Save` again so that client is updated.

As it's a confidential (non-public) application you need the secret for it. Click on `Credentials` and note the value of the `Secret` field.

Then edit `<WILDFLY HOME>/standalone/configuration/standalone.xml` and add the following to `<subsystem xmlns="urn:jboss:domain:keycloak:1.0">`:

    <secure-deployment name="customer-portal.war">
        <realm>demo</realm>
        <resource>customer-portal</resource>
        <credential name="secret">APPLICATION SECRET</credential>
    </secure-deployment>

In the above snippet replace the following:

* `APPLICATION SECRET` - replace with the applications secret you just noted from the Keycloak admin console

### Product Portal

Next deploy the product portal application.

Run the following to deploy it:

    # cd product-app
    # mvn install
    # cp target/product-portal.war <WILDFLY HOME>/standalone/deployments

Then open the Keycloak admin console to add a configuration for it. Navigate to the realm and click on `Clients` then `Add Client`. Fill in the form with:

* Client ID - `product-portal`

Then click on `Save`. You will see more possibilities to setup client now, so you can add the following:

`Access Type` - `confidential`
`Valid Redirect URIs` - `http://localhost:8080/product-portal/*` (click `Add` after filling in the field)

Then click on `Save` again so that client is updated.

It's a confidential (non-public) application, so we again need client credentials for it. But for product-portal, we will use authentication with signed JWT instead of traditional OAuth2 client secret.
Click on `Credentials` and fill the following values:

`Client Authenticator` - `Signed JWT`
`Use JWKS URL` - `ON`
`JWKS URL` - `/product-portal/k_jwks`

Then edit `<WILDFLY HOME>/standalone/configuration/standalone.xml` and add the following to `<subsystem xmlns="urn:jboss:domain:keycloak:1.0">`:

    <secure-deployment name="product-portal.war">
        <realm>demo</realm>
        <resource>product-portal</resource>
        <credential name="jwt">
          <client-keystore-file>classpath:keystore-client.jks</client-keystore-file>
          <client-keystore-type>JKS</client-keystore-type>
          <client-keystore-password>storepass</client-keystore-password>
          <client-key-password>keypass</client-key-password>
          <client-key-alias>clientkey</client-key-alias>
          <token-expiration>10</token-expiration>
        </credential>
    </secure-deployment>

With this configuration, the product-portal application will authenticate with JWT token signed by the private key from the file `keystore-client.jks`, which is available
inside the application WAR. If you don't use `classpath:` prefix in the configuration, you can use any keystore file from filesystem. If you want to generate your own keystore file,
you can either use `keytool` tool, but you can also generate the one inside Keycloak admin console and then save it locally.