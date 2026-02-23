package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

public class Oid4vcCredentialRequest extends AbstractHttpPostRequest<Oid4vcCredentialRequest, Oid4vcCredentialResponse> {

    private final CredentialRequest credRequest;

    Oid4vcCredentialRequest(AbstractOAuthClient<?> client, CredentialRequest credRequest) {
        super(client);
        this.credRequest = credRequest;
    }

    public Oid4vcCredentialRequest credentialConfigurationId(String credentialConfigurationId) {
        credRequest.setCredentialConfigurationId(credentialConfigurationId);
        return this;
    }

    public Oid4vcCredentialRequest credentialIdentifier(String credentialIdentifier) {
        credRequest.setCredentialIdentifier(credentialIdentifier);
        return this;
    }

    public Oid4vcCredentialRequest proofs(Proofs proofs) {
        credRequest.setProofs(proofs);
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredential();
    }

    @Override
    protected void initRequest() {
        if (credRequest != null) {
            String payload = JsonSerialization.valueAsString(credRequest);
            entity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        } else {
            // Trigger an empty payload in {@link AbstractHttpPostRequest#send()}.
            entity = new StringEntity("", ContentType.APPLICATION_JSON);
        }
    }

    @Override
    protected Oid4vcCredentialResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vcCredentialResponse(response);
    }
}
