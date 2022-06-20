CREATE DATABASE keycloak;
/* PSQLException: ERROR: setting or updating a password is not supported in insecure mode thrown when adding a password.
   this insecure mode is weird.
 */
CREATE user keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
