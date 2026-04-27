package org.keycloak.models;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretConstants {

    // client attribute names
    public static final String CLIENT_SECRET_ROTATION_ENABLED = "client.secret.rotation.enabled";
    public static final String CLIENT_SECRET_CREATION_TIME = "client.secret.creation.time";
    public static final String CLIENT_SECRET_EXPIRATION = "client.secret.expiration.time";
    public static final String CLIENT_ROTATED_SECRET = "client.secret.rotated";
    public static final String CLIENT_ROTATED_SECRET_CREATION_TIME = "client.secret.rotated.creation.time";
    public static final String CLIENT_ROTATED_SECRET_EXPIRATION_TIME = "client.secret.rotated.expiration.time";
    public static final String CLIENT_SECRET_REMAINING_EXPIRATION_TIME = "client.secret.remaining.expiration.time";

    /**
     * Attribute on the client, which specifies if client authentication is limited only to "client_secret_basic" (HTTP Basic authentication) or "client_secret_post" (Client
     * secret sent in the request parameters). If not filled, both methods are allowed
     */
    public static final String CLIENT_SECRET_AUTHENTICATION_ALLOWED_METHOD = "client.secret.authentication.allowed.method";

}
