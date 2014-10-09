package org.keycloak.adapters;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AdapterConstants {

    // URL endpoints
    public static final String K_LOGOUT = "k_logout";
    public static final String K_VERSION = "k_version";
    public static final String K_PUSH_NOT_BEFORE = "k_push_not_before";
    public static final String K_GET_USER_STATS = "k_get_user_stats";
    public static final String K_GET_SESSION_STATS = "k_get_session_stats";
    public static final String K_QUERY_BEARER_TOKEN = "k_query_bearer_token";

    // This param name is defined again in Keycloak Subsystem class
    // org.keycloak.subsystem.extensionKeycloakAdapterConfigDeploymentProcessor.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Undertow Integration.
    String AUTH_DATA_PARAM_NAME = "org.keycloak.json.adapterConfig";

    // Attribute passed in codeToToken request from adapter to Keycloak and saved in ClientSession. Contains ID of HttpSession on adapter
    public static final String HTTP_SESSION_ID = "http_session_id";

    // Attribute passed in codeToToken request from adapter to Keycloak and saved in ClientSession. Contains hostname of adapter where HttpSession is served
    public static final String HTTP_SESSION_HOST = "http_session_host";
}
