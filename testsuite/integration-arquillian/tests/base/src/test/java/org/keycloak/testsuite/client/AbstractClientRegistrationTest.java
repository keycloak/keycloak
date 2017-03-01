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
import org.junit.Before;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationTest extends AbstractKeycloakTest {

    static final String REALM_NAME = "test";

    ClientRegistration reg;

    @Before
    public void before() throws Exception {
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "test").build();
    }

    @After
    public void after() throws Exception {
        reg.close();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        rep.setId(REALM_NAME);
        rep.setRealm(REALM_NAME);
        rep.setUsers(new LinkedList<UserRepresentation>());

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        rep.getUsers().add(user);

        UserRepresentation user2 = new UserRepresentation();
        user2.setEnabled(true);
        user2.setUsername("create-clients");
        user2.setCredentials(credentials);
        user2.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));

        rep.getUsers().add(user2);

        UserRepresentation user3 = new UserRepresentation();
        user3.setEnabled(true);
        user3.setUsername("no-access");
        user3.setCredentials(credentials);

        rep.getUsers().add(user3);

        UserRepresentation appUser = new UserRepresentation();
        appUser.setEnabled(true);
        appUser.setUsername("test-user");
        appUser.setEmail("test-user@localhost");
        appUser.setCredentials(credentials);

        rep.getUsers().add(appUser);

        testRealms.add(rep);
    }

    public ClientRepresentation createClient(ClientRepresentation client) throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation response = reg.create(client);
        reg.auth(null);
        return response;
    }

    public ClientRepresentation getClient(String clientUuid) {
        try {
            return adminClient.realm(REALM_NAME).clients().get(clientUuid).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        }
    }

    void authCreateClients() {
        reg.auth(Auth.token(getToken("create-clients", "password")));
    }

    void authManageClients() {
        reg.auth(Auth.token(getToken("manage-clients", "password")));
    }

    void authNoAccess() {
        reg.auth(Auth.token(getToken("no-access", "password")));
    }

    private String getToken(String username, String password) {
        try {
            return oauth.doGrantAccessTokenRequest(REALM_NAME, username, password, null, Constants.ADMIN_CLI_CLIENT_ID, null).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
