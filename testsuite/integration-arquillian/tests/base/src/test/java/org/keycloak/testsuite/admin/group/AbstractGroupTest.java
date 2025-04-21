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

package org.keycloak.testsuite.admin.group;

import jakarta.ws.rs.core.Response;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.Details;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.security.PublicKey;
import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGroupTest extends AbstractKeycloakTest {

    protected String testRealmId;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        this.testRealmId = adminClient.realm(TEST).toRepresentation().getId();
    }

    AccessToken login(String login, String clientId, String clientSecret, String userId) throws Exception {
        AccessTokenResponse tokenResponse = oauth.client(clientId, clientSecret).doPasswordGrantRequest( login, "password");

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        PublicKey publicKey = PemUtils.decodePublicKey(KeyUtils.findActiveSigningKey(adminClient.realm("test")).getPublicKey());

        AccessToken accessTokenRepresentation = RSATokenVerifier.verifyToken(accessToken, publicKey, getAuthServerContextRoot() + "/auth/realms/test");

        JWSInput jws = new JWSInput(refreshToken);
        RefreshToken refreshTokenRepresentation = jws.readJsonContent(RefreshToken.class);

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessTokenRepresentation.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshTokenRepresentation.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        return accessTokenRepresentation;
    }

    RealmRepresentation loadTestRealm(List<RealmRepresentation> testRealms) {
        RealmRepresentation result = loadRealm("/testrealm.json");
        testRealms.add(result);
        return result;
    }

    GroupRepresentation createGroup(RealmResource realm, GroupRepresentation group) {
        try (Response response = realm.groups().add(group)) {
            String groupId = ApiUtil.getCreatedId(response);
            getCleanup().addGroupId(groupId);

            assertAdminEvents.assertEvent(testRealmId, OperationType.CREATE, AdminEventPaths.groupPath(groupId), group, ResourceType.GROUP);

            // Set ID to the original rep
            group.setId(groupId);
            return group;
        }
    }

    void addSubGroup(RealmResource realm, GroupRepresentation parent, GroupRepresentation child) {
        Response response = realm.groups().add(child);
        child.setId(ApiUtil.getCreatedId(response));
        response = realm.groups().group(parent.getId()).subGroup(child);
        response.close();
    }

    RoleRepresentation createRealmRole(RealmResource realm, RoleRepresentation role) {
        realm.roles().create(role);

        RoleRepresentation created = realm.roles().get(role.getName()).toRepresentation();
        getCleanup().addRoleId(created.getId());
        return created;
    }
}
