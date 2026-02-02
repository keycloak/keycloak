package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcCredentialRequest extends AbstractOid4vcRequest<Oid4vcCredentialRequest, Oid4vcCredentialResponse> {

    private final CredentialRequest body = new CredentialRequest();
    private boolean emptyBody = false;
    private String customBody = null;

    public Oid4vcCredentialRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public Oid4vcCredentialRequest credentialConfigurationId(String credentialConfigurationId) {
        body.setCredentialConfigurationId(credentialConfigurationId);
        return this;
    }

    public Oid4vcCredentialRequest credentialIdentifier(String credentialIdentifier) {
        body.setCredentialIdentifier(credentialIdentifier);
        return this;
    }

    public Oid4vcCredentialRequest proofs(Proofs proofs) {
        body.setProofs(proofs);
        return this;
    }

    /**
     * Set the request to send an empty payload body.
     * This is useful for testing edge cases where an empty body should be sent.
     */
    public Oid4vcCredentialRequest emptyBody() {
        this.emptyBody = true;
        this.customBody = null;
        return this;
    }

    /**
     * Set a custom raw body string for the request.
     * This is useful for testing edge cases like malformed JSON.
     * When set, this takes precedence over the structured body.
     *
     * @param body the custom body string to send
     * @return this request instance for method chaining
     */
    public Oid4vcCredentialRequest customBody(String body) {
        this.customBody = body;
        this.emptyBody = false;
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredential();
    }

    /**
     * Returns the request body. If {@link #customBody(String)} was called, returns the custom string.
     * If {@link #emptyBody()} was called, returns an empty string ("") to trigger an empty payload.
     * Otherwise, returns the {@link CredentialRequest} object to be serialized as JSON.
     *
     * @return the request body object, custom string, or empty string
     */
    @Override
    protected Object getBody() {
        if (customBody != null) {
            return customBody;
        }
        if (emptyBody) {
            return "";
        }
        return body;
    }

    @Override
    protected Oid4vcCredentialResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vcCredentialResponse(response);
    }
}
