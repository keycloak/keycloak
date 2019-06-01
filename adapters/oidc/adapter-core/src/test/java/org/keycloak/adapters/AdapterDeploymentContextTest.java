/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters;

import org.junit.Test;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.security.cert.X509Certificate;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AdapterDeploymentContextTest {

    @Test
    public void resolveUrls_ShouldResolveAllUrlsToRelativeFrontChannel_WhenRelativeFrontChannelUrlAndNoBackChannelUrlIsGiven() {
        AdapterConfig relativeAuthServeUrlConfig = createRelativeAuthServerConfig();

        KeycloakDeployment deployment = createDeployment();
        deployment.setAuthServerBaseUrl(relativeAuthServeUrlConfig);

        AdapterDeploymentContext deploymentContext = new AdapterDeploymentContext(deployment);
        String requestUri = "https://test.app";
        KeycloakDeployment resolvedDeployment = deploymentContext.resolveUrls(deployment, createFakeRequest(requestUri));

        String frontChannelUrl = requestUri + "/auth";
        assertEquals(frontChannelUrl, resolvedDeployment.getAuthServerBaseUrl());
        assertNull(resolvedDeployment.getAuthServerBackChannelBaseUrl());

        assertFrontChannelUrls(resolvedDeployment, frontChannelUrl);
        assertBackChannelUrls(resolvedDeployment, frontChannelUrl);
    }

    @Test
    public void resolveUrls_ShouldResolveBackChannelUrlsToBackChannel_WhenRelativeFrontChannelUrlAndBackChannelUrlIsGiven() {
        String backChannelUrl = "https://test.backchannel/auth";

        AdapterConfig relativeAuthServeUrlConfig = createRelativeAuthServerConfig();
        relativeAuthServeUrlConfig.setAuthServerBackChannelUrl(backChannelUrl);

        KeycloakDeployment deployment = createDeployment();
        deployment.setAuthServerBaseUrl(relativeAuthServeUrlConfig);
        deployment.setAuthServerBackChannelBaseUrl(relativeAuthServeUrlConfig);

        AdapterDeploymentContext deploymentContext = new AdapterDeploymentContext(deployment);
        String requestUri = "https://test.app";
        KeycloakDeployment resolvedDeployment = deploymentContext.resolveUrls(deployment, createFakeRequest(requestUri));

        String frontChannelUrl = requestUri + "/auth";
        assertEquals(frontChannelUrl, resolvedDeployment.getAuthServerBaseUrl());
        assertEquals(backChannelUrl, resolvedDeployment.getAuthServerBackChannelBaseUrl());

        assertFrontChannelUrls(resolvedDeployment, frontChannelUrl);
        assertBackChannelUrls(resolvedDeployment, backChannelUrl);
    }

    private void assertFrontChannelUrls(KeycloakDeployment deployment, String url) {
        assertEquals(url + "/realms/test", deployment.getRealmInfoUrl());
        assertEquals(url + "/realms/test/protocol/openid-connect/auth", deployment.getAuthUrl().build().toString());
        assertEquals(url + "/realms/test/account", deployment.getAccountUrl());
    }

    private void assertBackChannelUrls(KeycloakDeployment deployment, String url) {
        assertEquals(url + "/realms/test/protocol/openid-connect/token", deployment.getTokenUrl());
        assertEquals(url + "/realms/test/protocol/openid-connect/logout", deployment.getLogoutUrl().build().toString());
        assertEquals(url + "/realms/test/protocol/openid-connect/certs", deployment.getJwksUrl());
        assertEquals(url + "/realms/test/clients-managements/register-node", deployment.getRegisterNodeUrl());
        assertEquals(url + "/realms/test/clients-managements/unregister-node", deployment.getUnregisterNodeUrl());
    }

    private AdapterConfig createRelativeAuthServerConfig() {
        AdapterConfig relativeAuthServeUrlConfig = new AdapterConfig();
        relativeAuthServeUrlConfig.setAuthServerUrl("/auth");
        return relativeAuthServeUrlConfig;
    }

    private KeycloakDeployment createDeployment() {
        KeycloakDeployment deployment = new KeycloakDeployment();
        deployment.setRealm("test");
        return deployment;
    }

    private HttpFacade createFakeRequest(final String uri) {
        return new HttpFacade() {
            @Override
            public Request getRequest() {
                return new Request() {
                    @Override
                    public String getMethod() {
                        return null;
                    }

                    @Override
                    public String getURI() {
                        return uri;
                    }

                    @Override
                    public String getRelativePath() {
                        return null;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getFirstParam(String param) {
                        return null;
                    }

                    @Override
                    public String getQueryParamValue(String param) {
                        return null;
                    }

                    @Override
                    public Cookie getCookie(String cookieName) {
                        return null;
                    }

                    @Override
                    public String getHeader(String name) {
                        return null;
                    }

                    @Override
                    public List<String> getHeaders(String name) {
                        return null;
                    }

                    @Override
                    public InputStream getInputStream() {
                        return null;
                    }

                    @Override
                    public InputStream getInputStream(boolean buffered) {
                        return null;
                    }

                    @Override
                    public String getRemoteAddr() {
                        return null;
                    }

                    @Override
                    public void setError(AuthenticationError error) {

                    }

                    @Override
                    public void setError(LogoutError error) {

                    }
                };
            }

            @Override
            public Response getResponse() {
                return null;
            }

            @Override
            public X509Certificate[] getCertificateChain() {
                return new X509Certificate[0];
            }
        };
    }
}