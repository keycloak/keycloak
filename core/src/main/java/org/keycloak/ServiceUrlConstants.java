package org.keycloak;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ServiceUrlConstants {

    public static final String TOKEN_SERVICE_LOGIN_PATH = "/rest/realms/{realm-name}/tokens/login";
    public static final String TOKEN_SERVICE_ACCESS_CODE_PATH = "/rest/realms/{realm-name}/tokens/access/codes";
    public static final String TOKEN_SERVICE_REFRESH_PATH = "/rest/realms/{realm-name}/tokens/refresh";
}
