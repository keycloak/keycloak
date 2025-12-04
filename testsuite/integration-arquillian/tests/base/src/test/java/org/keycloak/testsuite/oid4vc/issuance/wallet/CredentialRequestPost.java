package org.keycloak.testsuite.oid4vc.issuance.wallet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import static org.keycloak.util.JsonSerialization.valueAsString;

public class CredentialRequestPost extends AbstractHttpPostRequest<CredentialRequestPost, CredentialResponse> {

    private final String endpointUri;

    public CredentialRequestPost(AbstractOAuthClient<?> client, String endpointUri, CredentialRequest credentialRequest) {
        super(client);
        this.endpointUri = endpointUri;
        this.entity = new StringEntity(valueAsString(credentialRequest), ContentType.APPLICATION_JSON);
    }

    @Override
    protected String getEndpoint() {
        return endpointUri;
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected CredentialResponse toResponse(CloseableHttpResponse response) throws IOException {

        int status = response.getStatusLine().getStatusCode();
        if (status != HttpServletResponse.SC_OK) {
            HttpEntity entity = response.getEntity();
            throw new IOException(String.format("[%d] Unexpected response: %s", status, EntityUtils.toString(entity)));
        }

        String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialResponse credentialResponse = JsonSerialization.valueFromString(s, CredentialResponse.class);

        return credentialResponse;
    }
}
