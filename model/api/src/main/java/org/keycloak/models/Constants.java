package org.keycloak.models;

import org.keycloak.OAuth2Constants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Constants {
    String ADMIN_CONSOLE_CLIENT_ID = "security-admin-console";
    String ADMIN_CLI_CLIENT_ID = "admin-cli";

    String ACCOUNT_MANAGEMENT_CLIENT_ID = "account";
    String IMPERSONATION_SERVICE_CLIENT_ID = "impersonation";
    String BROKER_SERVICE_CLIENT_ID = "broker";
    String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    String INSTALLED_APP_URN = "urn:ietf:wg:oauth:2.0:oob";
    String INSTALLED_APP_URL = "http://localhost";
    String READ_TOKEN_ROLE = "read-token";
    String[] BROKER_SERVICE_ROLES = {READ_TOKEN_ROLE};
    String OFFLINE_ACCESS_ROLE = OAuth2Constants.OFFLINE_ACCESS;

    String DEFAULT_HASH_ALGORITHM = "pbkdf2";

    // 15 minutes
    int DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT = 900;
    // 30 days
    int DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT = 2592000;

    String VERIFY_EMAIL_KEY = "VERIFY_EMAIL_KEY";
    String KEY = "key";

    // Prefix for user attributes used in various "context"data maps
    public static final String USER_ATTRIBUTES_PREFIX = "user.attributes.";
}
