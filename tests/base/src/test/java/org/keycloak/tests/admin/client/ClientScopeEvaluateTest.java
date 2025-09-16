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

package org.keycloak.testsuite.admin.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public class ClientScopeEvaluateTest extends AbstractClientTest {

    private ClientResource accountClient;

    @Before
    public void init() {
        accountClient = findClientResourceById("account");
        createTestUserWithAdminClient();
        getCleanup().addUserId(testUser.getId());
    }

    @Test
    public void testGenerateAccessToken() {
        AccessToken accessToken = accountClient.clientScopesEvaluate().generateAccessToken("openid", testUser.getId(), null);
        assertNotNull(accessToken);
        assertNotNull(accessToken.getSubject());
        assertNotNull(accessToken.getPreferredUsername());

        List<UserSessionRepresentation> sessions = accountClient.getUserSessions(0, 5);
        assertEquals(0, sessions.size());
    }

    @Test
    public void testGenerateIdToken() {
        IDToken idToken = accountClient.clientScopesEvaluate().generateExampleIdToken("openid", testUser.getId(), null);
        assertNotNull(idToken);
        assertNotNull(idToken.getSubject());
        assertNotNull(idToken.getPreferredUsername());
    }

    @Test
    public void testGenerateUserInfo() {
        Map<String, Object> userinfo = accountClient.clientScopesEvaluate().generateExampleUserinfo("openid", testUser.getId());
        assertFalse(userinfo.isEmpty());
        assertNotNull(userinfo.get(IDToken.SUBJECT));
        assertNotNull(userinfo.get(IDToken.PREFERRED_USERNAME));
    }

    @Test
    public void testGenerateAccessTokenWithoutBasicScope() {
        String basicScopeId = ApiUtil.findClientScopeByName(testRealmResource(),"basic").toRepresentation().getId();
        accountClient.removeDefaultClientScope(basicScopeId);

        AccessToken accessToken = accountClient.clientScopesEvaluate().generateAccessToken("openid", testUser.getId(), null);
        assertNotNull(accessToken);
        assertNull(accessToken.getSubject());

        accountClient.addDefaultClientScope(basicScopeId);
    }

    @Test
    public void testGenerateAccessTokenWithOptionalScope() {
        String emailScopeId = ApiUtil.findClientScopeByName(testRealmResource(),"email").toRepresentation().getId();
        accountClient.removeDefaultClientScope(emailScopeId);
        accountClient.addOptionalClientScope(emailScopeId);

        AccessToken accessToken = accountClient.clientScopesEvaluate().generateAccessToken("openid", testUser.getId(), null);
        assertNotNull(accessToken);
        assertNull(accessToken.getEmail());

        accessToken = accountClient.clientScopesEvaluate().generateAccessToken("openid email", testUser.getId(), null);
        assertNotNull(accessToken);
        assertNotNull(accessToken.getEmail());

        accountClient.removeOptionalClientScope(emailScopeId);
        accountClient.addDefaultClientScope(emailScopeId);
    }

}
