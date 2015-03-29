package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Details {

    String EMAIL = "email";
    String PREVIOUS_EMAIL = "previous_email";
    String UPDATED_EMAIL = "updated_email";
    String CODE_ID = "code_id";
    String REDIRECT_URI = "redirect_uri";
    String RESPONSE_TYPE = "response_type";
    String AUTH_METHOD = "auth_method";
    String IDENTITY_PROVIDER = "identity_provider";
    String IDENTITY_PROVIDER_IDENTITY = "identity_provider_identity";
    String IDENTITY_PROVIDER_ALIAS = "identity_provider_alias";
    String IDENTITY_PROVIDER_FACTORY = "identity_provider_factory";
    String IDENTITY_PROVIDER_FACTORY_IDENTITY = "identity_provider_factory_identity";
    String REGISTER_METHOD = "register_method";
    String USERNAME = "username";
    String REMEMBER_ME = "remember_me";
    String TOKEN_ID = "token_id";
    String REFRESH_TOKEN_ID = "refresh_token_id";
    String VALIDATE_ACCESS_TOKEN = "validate_access_token";
    String UPDATED_REFRESH_TOKEN_ID = "updated_refresh_token_id";
    String NODE_HOST = "node_host";
    String REASON = "reason";
    
    String REALM_ID = "realm_id";
    String REALM_NAME = "realm_name";
    String REALM_REQUIRED_SSL = "realm_required_ssl";
    
    String APPLICATION_CLUSTER_NODE = "application_cluster_node";
    
    String ROLE_ID = "role_id";
    String ROLE_NAME = "role_name";
    
    String PROVIDER_ID = "provider_id";
    String PROVIDER_NAME = "provider_name";
    
    String SERVER_VERSION = "server_version";
    String SERVER_TIME = "server_time";
    
}
