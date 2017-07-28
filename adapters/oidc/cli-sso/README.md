CLI Single Sign On
===================================

This java-based utility is meant for providing Keycloak integration to 
command line applications that are either written in Java or another language.  The
idea is that the Java app provided by this utility performs a login for a specific
client, parses responses, and exports an access token as an environment variable
that can be used by the command line utility you are accessing.

So, the idea is that you create a login script as follows:

    #!/bin/sh
    export KC_TOKEN_RESPONSE=`java -DKEYCLOAK_TOKEN_RESPONSE=$KC_TOKEN_RESPONSE -DKEYCLOAK_AUTH_SERVER=$KC_AUTH_SERVER -DKEYCLOAK_REALM=$KC_REALM -DKEYCLOAK_CLIENT=$KC_CLIENT -jar target/keycloak-cli-sso-3.3.0.CR1-SNAPSHOT.jar login`
    export KC_ACCESS_TOKEN=`java -DKEYCLOAK_TOKEN_RESPONSE=$KC_TOKEN_RESPONSE -jar target/keycloak-cli-sso-3.3.0.CR1-SNAPSHOT.jar token`
    

You pass in the parameters for connecting to Keycloak through Java system properties.  The command `login` will perform the following steps:
1. Look to see if `KEYCLOAK_TOKEN_RESPONSE` is set.  If so, it will check expiration information and perform a refresh token call if the token needs refreshing.
2. If `KEYCLOAK_TOKEN_RESPONSE` is not set, then it will use open a browser and perform a login to obtain an OAuth token response back.
3. The output to STDOUT will be the oauth accesss token response.  This will be stored by the script in an environment variable.
4. The 2nd line of the script extracts the access token from the response json and stuffs it in an environment variable that will be used by the CLI application.

So, the idea is that you wrap your CLI application within this script and extract the access token
from an environment variable.  For example, if our CLI application is a C program named `mycli` the script would look like this:

    #!/bin/sh
    export KC_TOKEN_RESPONSE=`java -DKEYCLOAK_TOKEN_RESPONSE=$KC_TOKEN_RESPONSE -DKEYCLOAK_AUTH_SERVER=$KC_AUTH_SERVER -DKEYCLOAK_REALM=$KC_REALM -DKEYCLOAK_CLIENT=$KC_CLIENT -jar target/keycloak-cli-sso-3.3.0.CR1-SNAPSHOT.jar login`
    export KC_ACCESS_TOKEN=`java -DKEYCLOAK_TOKEN_RESPONSE=$KC_TOKEN_RESPONSE -jar target/keycloak-cli-sso-3.3.0.CR1-SNAPSHOT.jar token`
    mycli $@
    
You would invoke it as follows:
    
    $ . mycli.sh

Using the `.` so that the environment variables get exported.
