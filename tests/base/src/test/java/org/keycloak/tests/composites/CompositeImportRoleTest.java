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
package org.keycloak.tests.composites;

import java.util.List;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class CompositeImportRoleTest extends AbstractCompositeKeycloakTest {

    @InjectRealm(fromJson = "/org/keycloak/tests/composites/testcomposite.json", config = CompositeImportRoleRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @Override
    protected ManagedRealm managedRealm() {
        return managedRealm;
    }

    @Test
    public void testAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.client("APP_COMPOSITE_APPLICATION", "password");
        oauth.doLogin("APP_COMPOSITE_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(getUserId("APP_COMPOSITE_USER"), token.getSubject());
        Assertions.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.client("APP_ROLE_APPLICATION", "password");
        oauth.doLogin("REALM_APP_COMPOSITE_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(getUserId("REALM_APP_COMPOSITE_USER"), token.getSubject());
        Assertions.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assertions.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppComposite() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_COMPOSITE_1_APPLICATION", "password");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());
        Assertions.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppRole() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_ROLE_1_APPLICATION", "password");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());
        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserRoleAppComposite() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_COMPOSITE_1_APPLICATION", "password");
        oauth.doLogin("REALM_ROLE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(getUserId("REALM_ROLE_1_USER"), token.getSubject());
        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    private static class CompositeImportRoleRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            RealmRepresentation testRealm = realm.build();

            if (testRealm.getClients() != null) {
                for (ClientRepresentation client : testRealm.getClients()) {
                    client.setRedirectUris(List.of("*"));
                }
            }

            if (testRealm.getApplications() != null) {
                for (ApplicationRepresentation application : testRealm.getApplications()) {
                    application.setRedirectUris(List.of("*"));
                }
            }

            if (testRealm.getUsers() != null) {
                for (UserRepresentation user : testRealm.getUsers()) {
                    if (user.getFirstName() == null) {
                        user.setFirstName("Test");
                    }
                    if (user.getLastName() == null) {
                        user.setLastName("User");
                    }
                }
            }

            return RealmBuilder.update(testRealm);
        }
    }
}
