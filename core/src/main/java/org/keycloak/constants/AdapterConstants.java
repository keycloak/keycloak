package org.keycloak.constants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AdapterConstants {

    // URL endpoints
    public static final String K_LOGOUT = "k_logout";
    public static final String K_VERSION = "k_version";
    public static final String K_PUSH_NOT_BEFORE = "k_push_not_before";
    public static final String K_TEST_AVAILABLE = "k_test_available";
    public static final String K_QUERY_BEARER_TOKEN = "k_query_bearer_token";

    // This param name is defined again in Keycloak Subsystem class
    // org.keycloak.subsystem.extensionKeycloakAdapterConfigDeploymentProcessor.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Undertow Integration.
    String AUTH_DATA_PARAM_NAME = "org.keycloak.json.adapterConfig";

    // Attribute passed in codeToToken request from adapter to Keycloak and saved in ClientSession. Contains ID of HttpSession on adapter
    public static final String APPLICATION_SESSION_STATE = "application_session_state";

    // Attribute passed in codeToToken request from adapter to Keycloak and saved in ClientSession. Contains hostname of adapter where HttpSession is served
    public static final String APPLICATION_SESSION_HOST = "application_session_host";

    // Attribute passed in registerNode request for register new application cluster node once he joined cluster
    public static final String APPLICATION_CLUSTER_HOST = "application_cluster_host";

    // Cookie used on adapter side to store token info. Used only when tokenStore is 'COOKIE'
    public static final String KEYCLOAK_ADAPTER_STATE_COOKIE = "KEYCLOAK_ADAPTER_STATE";

    // Request parameter used to specify the identifier of the identity provider that should be used to authenticate an user
    String KC_IDP_HINT = "kc_idp_hint";
}
