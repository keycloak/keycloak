package org.keycloak.adapters.authentication;

import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.BasicAuthHelper;

/**
 * Traditional OAuth2 authentication of clients based on client_id and client_secret
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientIdAndSecretCredentialsProvider implements ClientCredentialsProvider {

    private static Logger logger = Logger.getLogger(ClientCredentialsProviderUtils.class);

    public static final String PROVIDER_ID = CredentialRepresentation.SECRET;

    private String clientSecret;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(KeycloakDeployment deployment, Object config) {
        clientSecret = (String) config;
    }

    @Override
    public void setClientCredentials(KeycloakDeployment deployment, Map<String, String> requestHeaders, Map<String, String> formParams) {
        String clientId = deployment.getResourceName();

        if (!deployment.isPublicClient()) {
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                requestHeaders.put("Authorization", authorization);
            } else {
                logger.warnf("Client '%s' doesn't have secret available", clientId);
            }
        } else {
            formParams.put(OAuth2Constants.CLIENT_ID, clientId);
        }
    }
}
