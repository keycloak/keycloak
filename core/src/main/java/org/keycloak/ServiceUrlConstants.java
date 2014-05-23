package org.keycloak;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ServiceUrlConstants {

    public static final String TOKEN_SERVICE_LOGIN_PATH = "/realms/{realm-name}/tokens/login";
    public static final String TOKEN_SERVICE_ACCESS_CODE_PATH = "/realms/{realm-name}/tokens/access/codes";
    public static final String TOKEN_SERVICE_REFRESH_PATH = "/realms/{realm-name}/tokens/refresh";
    public static final String TOKEN_SERVICE_LOGOUT_PATH = "/realms/{realm-name}/tokens/logout";
    public static final String TOKEN_SERVICE_DIRECT_GRANT_PATH = "/realms/{realm-name}/tokens/grants/access";
    public static final String ACCOUNT_SERVICE_PATH = "/realms/{realm-name}/account";
    public static final String REALM_INFO_PATH = "/realms/{realm-name}";

}
