package org.keycloak.adapters;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AdapterConstants {

    // URL endpoints
    public static final String K_LOGOUT = "k_logout";
    public static final String K_QUERY_BEARER_TOKEN = "k_query_bearer_token";

    // This param name is defined again in Keycloak Subsystem class
    // org.keycloak.subsystem.extensionKeycloakAdapterConfigDeploymentProcessor.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Undertow Integration.
    String AUTH_DATA_PARAM_NAME = "org.keycloak.json.adapterConfig";
}
