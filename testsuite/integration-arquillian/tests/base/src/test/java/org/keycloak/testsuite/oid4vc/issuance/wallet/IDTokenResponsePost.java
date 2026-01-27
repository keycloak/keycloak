package org.keycloak.testsuite.oid4vc.issuance.wallet;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.client.cli.util.IoUtil;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;

public class IDTokenResponsePost extends AbstractHttpPostRequest<IDTokenResponsePost, String> {

    private final String endpointUri;

    public IDTokenResponsePost(AbstractOAuthClient<?> client, String endpointUri, IDTokenResponse idTokenResponse) {
        super(client);
        this.endpointUri = endpointUri;
        this.parameters.add(new BasicNameValuePair("id_token", idTokenResponse.idToken));
    }

    @Override
    protected void authorization() {
        // do nothing
    }

    @Override
    protected String getEndpoint() {
        return endpointUri;
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected String toResponse(CloseableHttpResponse response) throws IOException {

        String bodyAsText = IoUtil.readFully(response.getEntity().getContent());
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpServletResponse.SC_FOUND) {
            throw new IOException(String.format("[%d] Unexpected response: %s", status, bodyAsText));
        }

        String location = Optional.ofNullable(response.getFirstHeader("location"))
                .map(NameValuePair::getValue)
                .orElseThrow(() -> new IOException("No location header"));

        return location;
    }
}
