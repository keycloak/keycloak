package org.keycloak.tests.oid4vc.presentation;

import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.sdjwt.SdJwt;

class TestVpToken {

    private static final String MALFORMED_TOKEN = "dummy-vp-token";
    private static final String UNVERIFIABLE_PRESENTATION = "dummy-vp-token";

    private final String value;

    private TestVpToken(String value) {
        this.value = value;
    }

    static TestVpToken forCredential(AuthorizationRequest authorizationRequest, SdJwt credential) {
        return forPresentation(authorizationRequest, credential.toSdJwtString());
    }

    static TestVpToken unverifiablePresentation(AuthorizationRequest authorizationRequest) {
        return forPresentation(authorizationRequest, UNVERIFIABLE_PRESENTATION);
    }

    static TestVpToken malformed() {
        return new TestVpToken(MALFORMED_TOKEN);
    }

    String value() {
        return value;
    }

    private static TestVpToken forPresentation(AuthorizationRequest authorizationRequest, String presentation) {
        String credentialQueryId = authorizationRequest.getDcqlQuery().getCredentials().get(0).getId();
        return new TestVpToken("{\"" + credentialQueryId + "\":[\"" + presentation + "\"]}");
    }
}
