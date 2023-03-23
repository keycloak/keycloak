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

package org.keycloak.testsuite.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(Profile.Feature.DECLARATIVE_USER_PROFILE)
public class ServiceAccountUserProfileTest extends AbstractKeycloakTest {

    private static String userId;
    private static String userName;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmBuilder realm = RealmBuilder.create().name("test")
                .testEventListener();

        ClientRepresentation enabledApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl-refresh-on")
                .secret("secret1")
                .serviceAccountsEnabled(true)
                .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                .build();

        realm.client(enabledApp);

        ClientRepresentation enabledAppWithSkipRefreshToken = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl")
                .secret("secret1")
                .serviceAccountsEnabled(true)
                .build();

        realm.client(enabledAppWithSkipRefreshToken);

        ClientRepresentation disabledApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-disabled")
                .secret("secret1")
                .build();

        realm.client(disabledApp);

        ClientRepresentation secretsWithSpecialCharacterClient = ClientBuilder.create()
            .id(KeycloakModelUtils.generateId())
            .clientId("service-account-cl-special-secrets")
            .secret("secret/with=special?character")
            .serviceAccountsEnabled(true)
            .build();

        realm.client(secretsWithSpecialCharacterClient);

        UserBuilder defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-user@localhost");
        realm.user(defaultUser);

        userId = KeycloakModelUtils.generateId();
        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledApp.getClientId();

        UserBuilder serviceAccountUser = UserBuilder.create()
                .id(userId)
                .username(userName)
                .serviceAccountId(enabledApp.getClientId());
        realm.user(serviceAccountUser);

        RealmRepresentation realmRep = realm.build();
        VerifyProfileTest.enableDynamicUserProfile(realmRep);
        testRealms.add(realmRep);
    }

    @Test
    public void testDoNotUpdateUsername() {
        RealmResource test = adminClient.realm("test");
        RealmRepresentation realmRepresentation = test.toRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(true);
        test.update(realmRepresentation);
        UserResource serviceAccount = test.users().get(userId);
        UserRepresentation representation = serviceAccount.toRepresentation();
        String username = representation.getUsername();

        assertNull(representation.getEmail());

        serviceAccount.update(representation);
        representation = serviceAccount.toRepresentation();
        assertNull(representation.getEmail());
        assertEquals(username, representation.getUsername());

        representation.setEmail("test@keycloak.org");
        serviceAccount.update(representation);
        representation = serviceAccount.toRepresentation();
        assertNotNull(representation.getEmail());
        assertEquals(username, representation.getUsername());
    }

    @Test
    public void testSetAnyAttribute() {
        RealmResource test = adminClient.realm("test");
        UserResource serviceAccount = test.users().get(userId);
        UserRepresentation representation = serviceAccount.toRepresentation();

        representation.setAttributes(Map.of("attr-1", List.of("attr-1-value")));
        serviceAccount.update(representation);

        representation = serviceAccount.toRepresentation();
        assertFalse(representation.getAttributes().isEmpty());
        assertEquals("attr-1-value", representation.getAttributes().get("attr-1").get(0));
    }
}
