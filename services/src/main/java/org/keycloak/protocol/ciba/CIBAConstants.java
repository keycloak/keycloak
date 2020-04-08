package org.keycloak.protocol.ciba;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

public interface CIBAConstants {

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#auth_request
    // Authentication Request
    String SCOPE = OAuth2Constants.SCOPE;
    String CLIENT_NOTIFICATION_TOKEN = "client_notification_token";
    String ACR_VALUES = OIDCLoginProtocol.ACR_PARAM;
    String LOGIN_HINT_TOKEN = "login_hint_token";
    String ID_TOKEN_HINT = "id_token_hint"; // also defined in OIDC Core
    String LOGIN_HINT = OIDCLoginProtocol.LOGIN_HINT_PARAM;
    String BINDING_MESSAGE = "binding_message";
    String USER_CODE = "user_code";
    String REQUESTED_EXPIRY = "requested_expiry";

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#signed_auth_request
    // Signed Authentication Request
    String REQUEST = OIDCLoginProtocol.REQUEST_PARAM;

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#successful_authentication_request_acknowdlegment
    String AUTH_REQ_ID = "auth_req_id";
    String EXPIRES_IN = "expires_in";
    String INTERVAL = "interval";

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#token_request
    String GRANT_TYPE = OAuth2Constants.GRANT_TYPE;
    String GRANT_TYPE_VALUE = "urn:openid:params:grant-type:ciba";

    int INTERVAL_PENALTY = 5;
    int INTERVAL_UPPERBOUND = 600;
}
