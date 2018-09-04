package org.keycloak.protocol.oidc;

public interface OIDCConfigAttributes {

    String USER_INFO_RESPONSE_SIGNATURE_ALG = "user.info.response.signature.alg";

    String REQUEST_OBJECT_SIGNATURE_ALG = "request.object.signature.alg";

    String REQUEST_OBJECT_REQUIRED = "request.object.required";
    String REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI = "request or request_uri";
    String REQUEST_OBJECT_REQUIRED_REQUEST = "request only";
    String REQUEST_OBJECT_REQUIRED_REQUEST_URI = "request_uri only";

    String JWKS_URL = "jwks.url";

    String USE_JWKS_URL = "use.jwks.url";

    String EXCLUDE_SESSION_STATE_FROM_AUTH_RESPONSE = "exclude.session.state.from.auth.response";

    String USE_MTLS_HOK_TOKEN = "tls.client.certificate.bound.access.tokens";

    String ID_TOKEN_SIGNED_RESPONSE_ALG = "id.token.signed.response.alg";

    String ACCESS_TOKEN_SIGNED_RESPONSE_ALG = "access.token.signed.response.alg";

}
