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
    String REGISTER_METHOD = "register_method";
    String USERNAME = "username";
    String REMEMBER_ME = "remember_me";
    String TOKEN_ID = "token_id";
    String REFRESH_TOKEN_ID = "refresh_token_id";
    String VALIDATE_ACCESS_TOKEN = "validate_access_token";
    String UPDATED_REFRESH_TOKEN_ID = "updated_refresh_token_id";
    String NODE_HOST = "node_host";
}
