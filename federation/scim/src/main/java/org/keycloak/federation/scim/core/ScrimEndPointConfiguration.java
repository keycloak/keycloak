package org.keycloak.federation.scim.core;

import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import org.keycloak.component.ComponentModel;

public class ScrimEndPointConfiguration {
    // Configuration keys : also used in Admin Console page
    public static final String CONF_KEY_AUTH_MODE = "auth-mode";
    public static final String CONF_KEY_AUTH_PASSWORD = "auth-pass";
    public static final String CONF_KEY_AUTH_USER = "auth-user";
    public static final String CONF_KEY_CONTENT_TYPE = "content-type";
    public static final String CONF_KEY_ENDPOINT = "endpoint";
    public static final String CONF_KEY_SYNC_IMPORT_ACTION = "sync-import-action";
    public static final String CONF_KEY_SYNC_IMPORT = "sync-import";
    public static final String CONF_KEY_SYNC_REFRESH = "sync-refresh";
    public static final String CONF_KEY_PROPAGATION_USER = "propagation-user";
    public static final String CONF_KEY_PROPAGATION_GROUP = "propagation-group";
    public static final String CONF_KEY_LOG_ALL_SCIM_REQUESTS = "log-all-scim-requests";

    private final String endPoint;
    private final String id;
    private final String name;
    private final String contentType;
    private final String authorizationHeaderValue;
    private final ImportAction importAction;
    private final boolean pullFromScimSynchronisationActivated;
    private final boolean pushToScimSynchronisationActivated;
    private final boolean logAllScimRequests;

    public ScrimEndPointConfiguration(ComponentModel scimProviderConfiguration) {
        try {
            AuthMode authMode = AuthMode.valueOf(scimProviderConfiguration.get(CONF_KEY_AUTH_MODE));

            authorizationHeaderValue = switch (authMode) {
                case BEARER -> "Bearer " + scimProviderConfiguration.get(CONF_KEY_AUTH_PASSWORD);
                case BASIC_AUTH -> {
                    BasicAuth basicAuth = BasicAuth.builder().username(scimProviderConfiguration.get(CONF_KEY_AUTH_USER))
                            .password(scimProviderConfiguration.get(CONF_KEY_AUTH_PASSWORD)).build();
                    yield basicAuth.getAuthorizationHeaderValue();
                }
                case NONE -> "";
            };
            contentType = scimProviderConfiguration.get(CONF_KEY_CONTENT_TYPE, "");
            endPoint = scimProviderConfiguration.get(CONF_KEY_ENDPOINT, "");
            id = scimProviderConfiguration.getId();
            name = scimProviderConfiguration.getName();
            importAction = ImportAction.valueOf(scimProviderConfiguration.get(CONF_KEY_SYNC_IMPORT_ACTION));
            pullFromScimSynchronisationActivated = scimProviderConfiguration.get(CONF_KEY_SYNC_IMPORT, false);
            pushToScimSynchronisationActivated = scimProviderConfiguration.get(CONF_KEY_SYNC_REFRESH, false);
            logAllScimRequests = scimProviderConfiguration.get(CONF_KEY_LOG_ALL_SCIM_REQUESTS, false);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "authMode '" + scimProviderConfiguration.get(CONF_KEY_AUTH_MODE) + "' is not supported");
        }
    }

    public boolean isPushToScimSynchronisationActivated() {
        return pushToScimSynchronisationActivated;
    }

    public boolean isPullFromScimSynchronisationActivated() {
        return pullFromScimSynchronisationActivated;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAuthorizationHeaderValue() {
        return authorizationHeaderValue;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ImportAction getImportAction() {
        return importAction;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public boolean isLogAllScimRequests() {
        return logAllScimRequests;
    }

    public enum AuthMode {
        BEARER, BASIC_AUTH, NONE
    }

    public enum ImportAction {
        CREATE_LOCAL, DELETE_REMOTE, NOTHING
    }
}
