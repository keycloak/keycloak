package org.keycloak.admin.client;

import java.text.MessageFormat;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public enum URI {

    REALMS("/admin/realms"),
    REALM("/admin/realms/{0}"),
    REALM_ACCOUNT("/realms/{0}/account"),
    REALM_ACCOUNT_LOG("/realms/{0}/account/log"),
    REALM_ACCOUNT_PASSWORD("/realms/{0}/account/password"),
    REALM_ACCOUNT_SESSIONS_LOGOUT("/realms/{0}/account/sessions-logout"),
    REALM_ACCOUNT_SOCIAL("/realms/{0}/account/social"),
    REALM_ACCOUNT_TOTP("/realms/{0}/account/totp"),
    REALM_ACCOUNT_TOTP_REMOVE("/realms/{0}/account/totp-remove"),

    APP_SESSION_STATS("/admin/realms/{0}/application-session-stats"),
    APPS("/admin/realms/{0}/applications"),
    APP("/admin/realms/{0}/applications/{1}"),
    APP_ALLOWED_ORIGINS("/admin/realms/{0}/applications/{1}/allowed-origins"),
    APP_CLAIMS("/admin/realms/{0}/applications/{1}/claims"),
    APP_SECRET("/admin/realms/{0}/applications/{1}/client-secret"),
    APP_INSTALLATION_JBOSS("/admin/realms/{0}/applications/{1}/installation/jboss"),
    APP_INSTALLATION_JSON("/admin/realms/{0}/applications/{1}/installation/json"),
    APP_LOGOUT_ALL("/admin/realms/{0}/applications/{1}/logout-all"),
    APP_LOGOUT_USER("/admin/realms/{0}/applications/{1}/logout-user/{2}"),
    APP_PUSH_REVOCATION("/admin/realms/{0}/applications/{1}/push-revocation"),
    APP_ROLES("/admin/realms/{0}/applications/{1}/roles"),
    APP_ROLE("/admin/realms/{0}/applications/{1}/roles/{2}"),
    APP_ROLE_COMPOSITE("/admin/realms/{0}/applications/{1}/roles/{2}/composites"),
    APP_ROLE_COMPOSITE_APP("/admin/realms/{0}/applications/{1}/roles/{2}/composites/application/{3}"),
    APP_ROLE_COMPOSITE_REALM("/admin/realms/{0}/applications/{1}/roles/{2}/composites/realm"),
    APP_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings"),
    APP_APP_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings/applications/{2}"),
    APP_AVAILABLE_APP_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings/applications/{2}/available"),
    APP_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings/realm"),
    APP_AVAILABLE_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings/realm/available"),
    APP_EFFECTIVE_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/applications/{1}/scope-mappings/realm/composite"),
    APP_SESSION_COUNT("/admin/realms/{0}/applications/{1}/session-count"),
    APP_USER_SESSIONS("/admin/realms/{0}/applications/{1}/user-sessions"),

    AUDIT("/admin/realms/{0}/audit"),
    AUDIT_EVENTS("/admin/realms/{0}/audit/events"),

    LOGOUT_ALL("/admin/realms/{0}/logout-all"),
    OAUTH_CLIENTS("/admin/realms/{0}/oauth-clients"),
    OAUTH_CLIENT("/admin/realms/{0}/oauth-clients/{1}"),
    OAUTH_CLIENT_CLAIMS("/admin/realms/{0}/oauth-clients/{1}/claims"),
    OAUTH_CLIENT_SECRET("/admin/realms/{0}/oauth-clients/{1}/client-secret"),
    OAUTH_CLIENT_INSTALLATION_JSON("/admin/realms/{0}/oauth-clients/{1}/installation"),
    OAUTH_CLIENT_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings"),
    OAUTH_CLIENT_APP_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/applications/{2}"),
    OAUTH_CLIENT_AVAILABLE_APP_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/applications/{2}/available"),
    OAUTH_CLIENT_EFFECTIVE_APP_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/applications/{2}/composite"),
    OAUTH_CLIENT_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/realm"),
    OAUTH_CLIENT_AVAILABLE_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/realm/available"),
    OAUTH_CLIENT_EFFECTIVE_REALM_LEVEL_SCOPE_MAPPINGS("/admin/realms/{0}/oauth-clients/{1}/scope-mappings/realm/composite"),

    PUSH_REVOCATION("/admin/realms/{0}/push-revocation"),

    ROLES("/admin/realms/{0}/roles"),
    ROLE("/admin/realms/{0}/roles/{1}"),
    ROLE_COMPOSITE("/admin/realms/{0}/roles/{1}/composites"),
    ROLE_APP_LEVEL_COMPOSITE("/admin/realms/{0}/roles/{1}/composites/application/{2}"),
    ROLE_REALM_LEVEL_COMPOSITE("/admin/realms/{0}/roles/{1}/composites/realm"),
    ROLE_BY_ID("/admin/realms/{0}/roles-by-id/{1}"),
    ROLE_BY_ID_COMPOSITE("/admin/realms/{0}/roles-by-id/{1}/composites"),
    ROLE_BY_ID_APP_LEVEL_COMPOSITE("/admin/realms/{0}/roles-by-id/{1}/composites/application/{2}"),
    ROLE_BY_ID_REALM_LEVEL_COMPOSITE("/admin/realms/{0}/roles-by-id/{1}/composites/application/realm"),

    SESSION_STATS("/admin/realms/{1}/session-stats"),
    USER_SESSION("/admin/realms/{0}/sessions/{1}"),

    USERS("/admin/realms/{0}/users"),
    USER("/admin/realms/{0}/users/{1}"),
    USER_LOGOUT("/admin/realms/{0}/users/{1}/logout"),
    USER_REMOVE_TOTP("/admin/realms/{0}/users/{1}/remove-totp"),
    USER_RESET_PASSWORD("/admin/realms/{0}/users/{1}/reset-password"),
    USER_RESET_PASSWORD_EMAIL("/admin/realms/{0}/users/{1}/reset-password-email"),
    USER_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings"),
    USER_APP_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/applications/{2}"),
    USER_AVAILABLE_APP_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/applications/{2}/available"),
    USER_EFFECTIVE_APP_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/applications/{2}/composite"),
    USER_REALM_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/realm"),
    USER_AVAILABLE_REALM_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/realm/available"),
    USER_EFFECTIVE_REALM_LEVEL_ROLE_MAPPINGS("/admin/realms/{0}/users/{1}/role-mappings/realm/composite"),
    USER_SESSION_STATS("/admin/realms/{0}/users/{1}/session-stats"),
    USER_SESSIONS("/admin/realms/{0}/users/{1}/sessions"),
    USER_SOCIAL_LINKS("/admin/realms/{0}/users/{1}/social-links"),

    SERVER_INFO("/admin/serverinfo"),
    CONSOLE("/admin/{0}/console"),
    CONSOLE_CONFIG("/admin/{0}/console/config"),
    CONSOLE_JS("/admin/{0}/console/js/keycloak.js"),
    CONSOLE_LOGOUT("/admin/{0}/console/logout"),
    CONSOLE_PERMISSION_INFO("/admin/{0}/console/whoami"),
    CONSOLE_THEME_RESOURCES("/admin/{0}/console/{1}"),

    TOKENS_ACCESS_CODE("/realms/{0}/tokens/access/codes"),
    TOKENS_DIRECT_GRANT("/realms/{0}/tokens/grants/access"),
    TOKENS_REFRESH("/realms/{0}/tokens/refresh");

    private String uri;

    private URI(String uri) {
        this.uri = uri;
    }

    public String build(String serverUrl, String... params){
        return MessageFormat.format(serverUrl + uri, params);
    }

}
