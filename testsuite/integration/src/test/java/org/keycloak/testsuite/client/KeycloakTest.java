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

package org.keycloak.testsuite.client;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class KeycloakTest {

    static final String APP_CLIENT_ID = "keycloak-client-app";

    static final String REALM_ROLE_NAME = "realm-user";

    static final String CLIENT_ROLE_NAME = "client-user";

    @Rule
    public KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {

            ClientModel app = KeycloakModelUtils.createClient(adminstrationRealm, APP_CLIENT_ID);
            app.setEnabled(true);
            app.setSecret("secret");
            app.setFullScopeAllowed(true);
            app.setDirectAccessGrantsEnabled(true);
            app.addRole(CLIENT_ROLE_NAME);

            adminstrationRealm.addRole(REALM_ROLE_NAME);

            realmName = adminstrationRealm.getName();
        }
    });

    String rootUrl = "http://localhost:8081/auth";

    String realmName;

    Keycloak keycloak;

    @Before
    public void setup() {
        keycloak = KeycloakBuilder.builder()
                .realm(realmName)
                .serverUrl(rootUrl)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .username("admin")
                .password("admin")
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }

    @After
    public void teardown() {

        UsersResource usersResource = keycloak.realm(realmName).users();
        List<UserRepresentation> users = usersResource.search("user", null, null, null, 0, Integer.MAX_VALUE);

        for (UserRepresentation user : users) {
            usersResource.delete(user.getId());
        }
    }

    @Test
    public void createNewUserWithRealmAndClientRoles() {

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setRealmRoles(singletonList(REALM_ROLE_NAME));
        user.setClientRoles(singletonMap(APP_CLIENT_ID, singletonList(CLIENT_ROLE_NAME)));

        Response response = keycloak.realm(realmName).users().create(user);

        String userId = extractIdFromLocation(response);

        UserRepresentation userRep = keycloak.realm(realmName).users().get(userId).toRepresentation();

        List<String> clientRoles = userRep.getClientRoles().get(APP_CLIENT_ID);
        Assert.assertNotNull("clientRoles must not be null!", clientRoles);
        Assert.assertEquals(clientRoles, Arrays.asList(CLIENT_ROLE_NAME));

        List<String> realmRoles = userRep.getRealmRoles();
        Assert.assertNotNull("realmRoles must not be null!", realmRoles);
        Assert.assertTrue("Realm roles should contain: " + REALM_ROLE_NAME, realmRoles.contains(REALM_ROLE_NAME));
    }

    private static String extractIdFromLocation(Response response) {

        Assert.assertNotNull("Response must not be null!", response);

        return response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);
    }
}
