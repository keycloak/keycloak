package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcCredentialRequest extends AbstractOid4vcRequest<Oid4vcCredentialRequest, Oid4vcCredentialResponse> {

    private final CredentialRequest body = new CredentialRequest();
    private boolean emptyBody = false;

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
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredential();
    }

    /**
     * Returns the request body. If {@link #emptyBody()} was called, returns an empty string ("")
     * to trigger an empty payload in {@link AbstractOid4vcRequest#send()}.
     * If not, returns the {@link CredentialRequest} object to be serialized as JSON.
     *
     * @return the request body object or empty string
     */
    @Override
    protected Object getBody() {
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
