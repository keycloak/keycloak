/*
 * Copyright 2025 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
@KeycloakIntegrationTest
public class ClientScopeEvaluateTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = ClientScopeUserConfig.class)
    ManagedUser managedUser;

    @InjectClient
    ManagedClient managedClient;

    @Test
    public void testGenerateAccessToken() {
        AccessToken accessToken = managedClient.admin().clientScopesEvaluate().generateAccessToken("openid", managedUser.getId(), null);
        assertNotNull(accessToken);
        assertNotNull(accessToken.getSubject());
        assertNotNull(accessToken.getPreferredUsername());

        List<UserSessionRepresentation> sessions = managedClient.admin().getUserSessions(0, 5);
        assertEquals(0, sessions.size());
    }

    @Test
    public void testGenerateIdToken() {
        IDToken idToken = managedClient.admin().clientScopesEvaluate().generateExampleIdToken("openid", managedUser.getId(), null);
        assertNotNull(idToken);
        assertNotNull(idToken.getSubject());
        assertNotNull(idToken.getPreferredUsername());
    }

    @Test
    public void testGenerateUserInfo() {
        Map<String, Object> userinfo = managedClient.admin().clientScopesEvaluate().generateExampleUserinfo("openid", managedUser.getId());
        assertFalse(userinfo.isEmpty());
        assertNotNull(userinfo.get(IDToken.SUBJECT));
        assertNotNull(userinfo.get(IDToken.PREFERRED_USERNAME));
    }

    @Test
    public void testGenerateAccessTokenWithoutBasicScope() {
        String basicScopeId = AdminApiUtil.findClientScopeByName(managedRealm.admin(),"basic").toRepresentation().getId();
        managedClient.admin().removeDefaultClientScope(basicScopeId);

        AccessToken accessToken = managedClient.admin().clientScopesEvaluate().generateAccessToken("openid", managedUser.getId(), null);
        assertNotNull(accessToken);
        assertNull(accessToken.getSubject());

        managedClient.admin().addDefaultClientScope(basicScopeId);
    }

    @Test
    public void testGenerateAccessTokenWithOptionalScope() {
        String emailScopeId = AdminApiUtil.findClientScopeByName(managedRealm.admin(),"email").toRepresentation().getId();
        managedClient.admin().removeDefaultClientScope(emailScopeId);
        managedClient.admin().addOptionalClientScope(emailScopeId);

        AccessToken accessToken = managedClient.admin().clientScopesEvaluate().generateAccessToken("openid", managedUser.getId(), null);
        assertNotNull(accessToken);
        assertNull(accessToken.getEmail());

        accessToken = managedClient.admin().clientScopesEvaluate().generateAccessToken("openid email", managedUser.getId(), null);
        assertNotNull(accessToken);
        assertNotNull(accessToken.getEmail());

        managedClient.admin().removeOptionalClientScope(emailScopeId);
        managedClient.admin().addDefaultClientScope(emailScopeId);
    }

    private static class ClientScopeUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("test-user")
                    .name("Test", "User")
                    .email("test@user.com")
                    .emailVerified(true);
        }
    }
}
