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

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.UserProfileConstants;
import org.keycloak.validate.validators.EmailValidator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServiceAccountUserProfileTest extends AbstractKeycloakTest {

    private static String userId;
    private static String userName;

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

        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledApp.getClientId();

        UserBuilder serviceAccountUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username(userName)
                .serviceAccountId(enabledApp.getClientId());
        realm.user(serviceAccountUser);

        RealmRepresentation realmRep = realm.build();
        testRealms.add(realmRep);
    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userId = adminClient.realm("test").users().search(userName, true).get(0).getId();
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

        assertNotNull(username);
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

        Map<String, List<String>> unmanagedAttributes = test.users().get(userId).getUnmanagedAttributes();

        assertEquals(1, unmanagedAttributes.size());
    }

    @Test
    public void testEmailFormatIsEnforced() {
        final RealmResource realm = adminClient.realm("test");
        UserResource serviceAccount = realm.users().get(userId);
        UserRepresentation rep = serviceAccount.toRepresentation();
        rep.setEmail("invalidEmail");
        BadRequestException e = assertThrows(BadRequestException.class, () -> serviceAccount.update(rep));
        assertThat(e.getResponse().readEntity(String.class), containsString(EmailValidator.MESSAGE_INVALID_EMAIL));
    }

    @Test
    public void testAttributesAreNotRequired() {
        final RealmResource realm = adminClient.realm("test");
        UPConfig config = realm.users().userProfile().getConfiguration();
        UPAttribute lastName = config.getAttribute(UserModel.LAST_NAME);
        assertNotNull("The attribute lastName is not defined in User Profile", lastName);
        UPAttributeRequired upRequired = new UPAttributeRequired(Set.of(
                UserProfileConstants.ROLE_ADMIN, UserProfileConstants.ROLE_USER), null);
        lastName.setRequired(upRequired);
        realm.users().userProfile().update(config);
        getCleanup().addCleanup(() -> realm.users().userProfile().update(null));

        UserResource serviceAccount = realm.users().get(userId);
        UserRepresentation rep = serviceAccount.toRepresentation();
        rep.setLastName(null);
        serviceAccount.update(rep);
    }
}
