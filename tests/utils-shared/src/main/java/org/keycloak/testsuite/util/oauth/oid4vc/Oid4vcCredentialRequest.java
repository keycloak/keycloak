package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

public class Oid4vcCredentialRequest extends AbstractHttpPostRequest<Oid4vcCredentialRequest, Oid4vcCredentialResponse> {

    private CredentialRequest credRequest;

    public Oid4vcCredentialRequest(AbstractOAuthClient<?> client, CredentialRequest credRequest) {
        super(client);
        this.credRequest = credRequest;
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
