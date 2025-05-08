/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.client;

import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Pascal Knüppel
 */
public class ClientScopeTests extends AbstractKeycloakTest {

    private static String realmName;

    private static String clientUUID;
    private static String clientId;
    private static String clientSecret;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        realmName = "test";
        RealmBuilder realm = RealmBuilder.create().name(realmName)
                                         .testEventListener();

        clientId = "service-account-cl";
        clientSecret = "secret1";
        ClientRepresentation enabledAppWithSkipRefreshToken = ClientBuilder.create()
                                                                           .clientId(clientId)
                                                                           .secret(clientSecret)
                                                                           .serviceAccountsEnabled(true)
                                                                           .build();
        realm.client(enabledAppWithSkipRefreshToken);

        UserBuilder defaultUser = UserBuilder.create()
                                             .id(KeycloakModelUtils.generateId())
                                             .username("test-user@localhost")
                                             .password("password")
                                             .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                                             .addRoles(OAuth2Constants.OFFLINE_ACCESS);
        realm.user(defaultUser);

        testRealms.add(realm.build());
    }

    @Override
    public void importRealm(RealmRepresentation realm) {
        super.importRealm(realm);
        if (Objects.equals(realm.getRealm(), realmName)) {
            clientUUID = adminClient.realm(realmName).clients().findByClientId(clientId).get(0).getId();
        }
    }

    @Test
    public void createClientScopeWithoutProtocol() throws IOException {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(null); // this should cause a BadRequestException
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        final RealmResource testRealm = adminClient.realm(realmName);
        try (Response response = testRealm.clientScopes().create(clientScope)) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            String errorMessage = response.readEntity(String.class);
            Assert.assertTrue(errorMessage.contains("Unexpected protocol"));
        }
    }

    @Test
    public void createClientScopeWithOpenIdProtocol() throws IOException {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol("openid-connect");
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        final RealmResource testRealm = adminClient.realm(realmName);

        String clientScopeId = null;
        try {
            clientScopeId = createClientScope(realmName, clientScope);
        } finally {
            // cleanup
            testRealm.clientScopes().get(clientScopeId).remove();
        }
    }

    @Test
    public void createClientScopeWithSamlProtocol() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol("saml");
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        final RealmResource testRealm = adminClient.realm(realmName);

        String clientScopeId = null;
        try {
            clientScopeId = createClientScope(realmName, clientScope);
        } finally {
            // cleanup
            testRealm.clientScopes().get(clientScopeId).remove();
        }
    }

    @Test
    public void createClientScopeWithOpenId4VCIProtocol() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol("oid4vc");
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        final RealmResource testRealm = adminClient.realm(realmName);

        String clientScopeId = null;
        try {
            clientScopeId = createClientScope(realmName, clientScope);
        } finally {
            // cleanup
            testRealm.clientScopes().get(clientScopeId).remove();
        }
    }

}
