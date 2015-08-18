package org.keycloak;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface OAuth2Constants {

    String CODE = "code";

    String CLIENT_ID = "client_id";

    String ERROR = "error";

    String ERROR_DESCRIPTION = "error_description";

    String REDIRECT_URI = "redirect_uri";

    String SCOPE = "scope";

    String STATE = "state";

    String GRANT_TYPE = "grant_type";

    String RESPONSE_TYPE = "response_type";

    String REFRESH_TOKEN = "refresh_token";

    String AUTHORIZATION_CODE = "authorization_code";

    String PASSWORD = "password";

    String CLIENT_CREDENTIALS = "client_credentials";

    // https://tools.ietf.org/html/draft-ietf-oauth-assertions-01#page-5
    String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    String CLIENT_ASSERTION = "client_assertion";

    // https://tools.ietf.org/html/draft-jones-oauth-jwt-bearer-03#section-2.2
    String CLIENT_ASSERTION_TYPE_JWT = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

}


