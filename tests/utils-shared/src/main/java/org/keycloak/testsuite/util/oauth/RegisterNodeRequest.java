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
public class RegisterNodeRequest extends AbstractHttpPostRequest<RegisterNodeRequest, RegisterNodeResponse> {

    private String clientClusterHost;

    RegisterNodeRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return UriBuilder.fromUri(client.getBaseUrl())
                .path(RealmsResource.class)
                .path("{realm}/clients-managements")
                .path(ClientsManagementService.class, "registerNode")
                .build(client.getRealm())
                .toString();
    }

    public RegisterNodeRequest clientClusterHost(String clientClusterHost) {
        this.clientClusterHost = clientClusterHost;
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(AdapterConstants.CLIENT_CLUSTER_HOST, clientClusterHost);
    }

    @Override
    protected RegisterNodeResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new RegisterNodeResponse(response);
    }
}
