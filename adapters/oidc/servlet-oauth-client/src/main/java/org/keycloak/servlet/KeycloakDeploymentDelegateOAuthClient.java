/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.servlet;

import java.util.Map;

import org.keycloak.AbstractOAuthClient;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.enums.RelativeUrlsUsed;

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
        throw new IllegalStateException("Illegal to call this method. Use KeycloakDeployment to resolve correct deployment for this request");
    }

    @Override
    public void setAuthUrl(String authUrl) {
        throw new IllegalStateException("Illegal to call this method");
    }

    @Override
    public String getTokenUrl() {
        throw new IllegalStateException("Illegal to call this method. Use KeycloakDeployment to resolve correct deployment for this request");
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
