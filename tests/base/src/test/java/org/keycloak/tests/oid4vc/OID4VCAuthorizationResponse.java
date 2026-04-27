package org.keycloak.tests.oid4vc;


import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

public class OID4VCAuthorizationResponse extends AuthorizationEndpointResponse {

    public OID4VCAuthorizationResponse(String responseUrl, String responseType, String responseMode, String redirectUri) {
        super(responseUrl, responseType, responseMode, redirectUri);
    }

    public OID4VCAuthorizationResponse(AuthorizationEndpointResponse authResponse) {
        super(authResponse.getParams());
    }

    public String getVpToken() {
        return params.get("vp_token");
    }
}
