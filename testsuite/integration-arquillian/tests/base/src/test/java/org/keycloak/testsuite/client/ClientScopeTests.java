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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author Pascal Knüppel
 */
public class ClientScopeTests extends AbstractKeycloakTest {

    private static String realmName;

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        realmName = "test";
        RealmBuilder realm = RealmBuilder.create().name(realmName);
        testRealms.add(realm.build());
    }


    @Test
    public void createClientScopeWithoutProtocol() {
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
    public void createClientScopeWithOpenIdProtocol() {
        createClientScope("openid-connect");
    }

    @Test
    public void createClientScopeWithSamlProtocol() {
        createClientScope("saml");
    }

    @Test
    public void createClientScopeWithOpenId4VCIProtocol() {
        createClientScope("oid4vc");
    }

    private void createClientScope(String protocol) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(protocol);
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
