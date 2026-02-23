package org.keycloak.testsuite.rest.representation;

import org.keycloak.representations.oidc.OIDCClientRepresentation;

public class TestClientIdMetadataDocumentRequest {

    private OIDCClientRepresentation oidcClientRepresentation;
    private String rawCacheControlHeaderValue;
    private String command;

    public TestClientIdMetadataDocumentRequest(OIDCClientRepresentation oidcClientRepresentation, String rawCacheControlHeaderValue, String command) {
        this.oidcClientRepresentation = oidcClientRepresentation;
        this.rawCacheControlHeaderValue = rawCacheControlHeaderValue;
        this.command = command;
    }

    public OIDCClientRepresentation getOidcClientRepresentation() {
        return oidcClientRepresentation;
    }

    public void setOidcClientRepresentation(OIDCClientRepresentation oidcClientRepresentation) {
        this.oidcClientRepresentation = oidcClientRepresentation;
    }

    public String getRawCacheControlHeaderValue() {
        return rawCacheControlHeaderValue;
    }

    public void setRawCacheControlHeaderValue(String rawCacheControlHeaderValue) {
        this.rawCacheControlHeaderValue = rawCacheControlHeaderValue;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

}
