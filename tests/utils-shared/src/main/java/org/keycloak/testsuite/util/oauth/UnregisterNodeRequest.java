package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.constants.AdapterConstants;
import org.keycloak.services.resources.ClientsManagementService;
import org.keycloak.services.resources.RealmsResource;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 *
 * @author rmartinc
 */
public class UnregisterNodeRequest extends AbstractHttpPostRequest<UnregisterNodeRequest, UnregisterNodeResponse> {

    private String clientClusterHost;

    UnregisterNodeRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return UriBuilder.fromUri(client.getBaseUrl())
                .path(RealmsResource.class)
                .path("{realm}/clients-managements")
                .path(ClientsManagementService.class, "unregisterNode")
                .build(client.getRealm())
                .toString();
    }

    public UnregisterNodeRequest clientClusterHost(String clientClusterHost) {
        this.clientClusterHost = clientClusterHost;
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(AdapterConstants.CLIENT_CLUSTER_HOST, clientClusterHost);
    }

    @Override
    protected UnregisterNodeResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new UnregisterNodeResponse(response);
    }
}
