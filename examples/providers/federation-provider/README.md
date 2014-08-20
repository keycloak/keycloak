Example User Federation Provider
===================================================

This is an example of user federation backed by a simple properties file.  This properties file only contains username/password
key pairs.  To deploy, build this directory then take the jar and copy it to the WEB-INF/lib of the keycloak server's
WAR file.  You will then have to restart the authentication server.

The ClasspathPropertiesFederationProvider is an example of a readonly provider.  If you go to the Users/Federation
  page of the admin console you will see this provider listed under "classpath-properties.  To configure this provider you 
specify a classpath to a properties file in the "path" field of the admin page for this plugin.  This example includes
a "test-users.properties" within the JAR that you can use as the variable.
  
The FilePropertiesFederationProvider is an example of a writable provider.  It synchronizes changes made to
username and password with the properties file.  If you go to the Users/Federation page of the admin console you will 
see this provider listed under "file-properties".  To configure this provider you specify a fully qualified file path to 
a properties file in the "path" field of the admin page for this plugin.  