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

package org.keycloak.adapters;

import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.junit.Test;
import org.keycloak.adapters.authentication.ClientIdAndSecretCredentialsProvider;
import org.keycloak.adapters.authentication.JWTClientCredentialsProvider;
import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.enums.TokenStore;
import org.keycloak.common.util.PemUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakDeploymentBuilderTest {

    @Test
    public void load() throws Exception {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/keycloak.json"));
        assertEquals("demo", deployment.getRealm());
        assertEquals("customer-portal", deployment.getResourceName());
        assertEquals(PemUtils.decodePublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB"), deployment.getRealmKey());
        assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/auth", deployment.getAuthUrl().build().toString());
        assertEquals(SslRequired.EXTERNAL, deployment.getSslRequired());
        assertTrue(deployment.isUseResourceRoleMappings());
        assertTrue(deployment.isCors());
        assertEquals(1000, deployment.getCorsMaxAge());
        assertEquals("POST, PUT, DELETE, GET", deployment.getCorsAllowedMethods());
        assertEquals("X-Custom, X-Custom2", deployment.getCorsAllowedHeaders());
        assertTrue(deployment.isBearerOnly());
        assertTrue(deployment.isPublicClient());
        assertTrue(deployment.isEnableBasicAuth());
        assertTrue(deployment.isExposeToken());
        assertEquals("234234-234234-234234", deployment.getResourceCredentials().get("secret"));
        assertEquals(ClientIdAndSecretCredentialsProvider.PROVIDER_ID, deployment.getClientAuthenticator().getId());
        assertEquals(20, ((ThreadSafeClientConnManager) deployment.getClient().getConnectionManager()).getMaxTotal());
        assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/token", deployment.getTokenUrl());
        assertEquals(RelativeUrlsUsed.NEVER, deployment.getRelativeUrls());
        assertTrue(deployment.isAlwaysRefreshToken());
        assertTrue(deployment.isRegisterNodeAtStartup());
        assertEquals(1000, deployment.getRegisterNodePeriod());
        assertEquals(TokenStore.COOKIE, deployment.getTokenStore());
        assertEquals("email", deployment.getPrincipalAttribute());
        assertEquals(10, deployment.getTokenMinimumTimeToLive());
    }

    @Test
    public void loadNoClientCredentials() throws Exception {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/keycloak-no-credentials.json"));
        assertEquals(ClientIdAndSecretCredentialsProvider.PROVIDER_ID, deployment.getClientAuthenticator().getId());
    }

    @Test
    public void loadJwtCredentials() throws Exception {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/keycloak-jwt.json"));
        assertEquals(JWTClientCredentialsProvider.PROVIDER_ID, deployment.getClientAuthenticator().getId());
    }

}
