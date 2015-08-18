package org.keycloak.servlet;

import java.util.Map;

import org.keycloak.AbstractOAuthClient;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.enums.RelativeUrlsUsed;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakDeploymentDelegateOAuthClient extends AbstractOAuthClient {

    private KeycloakDeployment deployment;

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public void setDeployment(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public String getClientId() {
        return deployment.getResourceName();
    }

    @Override
    public void setClientId(String clientId) {
        deployment.setResourceName(clientId);
    }

    @Override
    public Map<String, Object> getCredentials() {
        return deployment.getResourceCredentials();
    }

    @Override
    public void setCredentials(Map<String, Object> credentials) {
        deployment.setResourceCredentials(credentials);
    }

    @Override
    public String getAuthUrl() {
        return deployment.getAuthUrl().clone().build().toString();
    }

    @Override
    public void setAuthUrl(String authUrl) {
        throw new IllegalStateException("Illegal to call this method");
    }

    @Override
    public String getTokenUrl() {
        return deployment.getTokenUrl();
    }

    @Override
    public void setTokenUrl(String tokenUrl) {
        throw new IllegalStateException("Illegal to call this method");
    }

    @Override
    public boolean isPublicClient() {
        return deployment.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean publicClient) {
        deployment.setPublicClient(publicClient);
    }

    @Override
    public RelativeUrlsUsed getRelativeUrlsUsed() {
        return deployment.getRelativeUrls();
    }

    @Override
    public void setRelativeUrlsUsed(RelativeUrlsUsed relativeUrlsUsed) {
        throw new IllegalStateException("Illegal to call this method");
    }
}
