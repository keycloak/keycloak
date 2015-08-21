package org.keycloak.adapters.authentication;

import java.util.Map;

import org.keycloak.adapters.KeycloakDeployment;

/**
 * TODO: Javadoc
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientCredentialsProvider {

    String getId();

    void init(KeycloakDeployment deployment, Object config);

    void setClientCredentials(KeycloakDeployment deployment, Map<String, String> requestHeaders, Map<String, String> formParams);
}
