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

package org.keycloak.testsuite.adduser;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.util.JsonSerialization;
import org.keycloak.wildfly.adduser.AddUser;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.*;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class AddUserTest extends AbstractKeycloakTest {

    @ArquillianResource
    private ContainerController controller;

    @BeforeClass
    public static void enabled() {
        // don't run with auth-server-undertow for now
        ContainerAssume.assumeNotAuthServerUndertow();

        // container auth-server-remote cannot be restarted
        ContainerAssume.assumeNotAuthServerRemote();
    }

    @Test
    public void addUserTest() throws Exception {
        final String username = "addusertest-admin";
        final String realmName = "master";
        final String configDir = System.getProperty("auth.server.config.dir");
        assertThat("AuthServer config directory is NULL !!", configDir, notNullValue());

        String authServerQualifier = suiteContext.getAuthServerInfo().getQualifier();
        assertThat("Qualifier of AuthServer is empty or NULL !!", authServerQualifier, not(isEmptyOrNullString()));
        assertThat("Controller isn't running.", controller.isStarted(authServerQualifier), is(true));

        AddUser.main(new String[]{"-u", username, "-p", "password", "--sc", configDir});

        //Read keycloak-add-user.json
        List<RealmRepresentation> realms = JsonSerialization.readValue(new FileInputStream(new File(configDir, "keycloak-add-user.json")),
                new TypeReference<List<RealmRepresentation>>() {
                });

        assertThat("File 'keycloak-add-user.json' is empty.", realms, not(empty()));

        //-----------------Get-Indexes-------------------//
        int realmIndex = getRealmIndex(realmName, realms);
        assertThat("Realm " + realmName + " not found.", realmIndex, is(not(-1)));

        int userIndex = getUserIndex(username, realms.get(realmIndex).getUsers());
        assertThat("User " + username + " not found", userIndex, is(not(-1)));


        UserRepresentation user = realms.get(realmIndex).getUsers().get(userIndex);
        assertThat("Username from Json file is wrong.", user.getUsername(), is(username));

        //------------------Credentials-----------------------------//
        assertThat("User Credentials are NULL", user.getCredentials().get(0), notNullValue());
        CredentialRepresentation credentials = user.getCredentials().get(0);
        PasswordCredentialModel pcm = PasswordCredentialModel.createFromCredentialModel(RepresentationToModel.toModel(credentials));
        assertThat("User Credentials have wrong Algorithm.", pcm.getPasswordCredentialData().getAlgorithm(), is(Pbkdf2Sha256PasswordHashProviderFactory.ID));
        assertThat("User Credentials have wrong Hash Iterations", pcm.getPasswordCredentialData().getHashIterations(), is(100000));

        //------------------Restart--Container---------------------//
        controller.stop(authServerQualifier);
        controller.start(authServerQualifier);

        RealmResource realmResource = getAdminClient().realm(realmName);
        assertThat("Realm resource is NULL !!", realmResource, notNullValue());

        user = realmResource.users().search(username).get(0);
        assertThat("Username is wrong.", user.getUsername(), is(username));

        UserResource userResource = realmResource.users().get(user.getId());
        assertThat("User resource is NULL !!", userResource, notNullValue());

        //--------------Roles-----------------------//
        try {
            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listAll();

            assertRoles(realmRoles, "admin", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);

            List<ClientRepresentation> clients = realmResource.clients().findAll();
            String accountId = null;
            for (ClientRepresentation c : clients) {
                if (c.getClientId().equals("account")) {
                    accountId = c.getId();
                }
            }

            List<RoleRepresentation> accountRoles = userResource.roles().clientLevel(accountId).listAll();
            assertRoles(accountRoles, "view-profile", "manage-account");
        } finally {
            userResource.remove();
        }
    }

    private int getUserIndex(String userName, List<UserRepresentation> list) {
        assertThat("Parameter 'list' is NULL.", list, notNullValue());
        assertThat("List is empty.", list.isEmpty(), is(false));

        for (UserRepresentation u : list) {
            if (u.getUsername().equals(userName))
                return list.indexOf(u);
        }
        return -1;
    }

    private int getRealmIndex(String realmName, List<RealmRepresentation> list) {
        assertThat("Parameter 'list' is NULL.", list, notNullValue());
        assertThat("List is empty.", list.isEmpty(), is(false));

        for (RealmRepresentation u : list) {
            if (u.getRealm().equals(realmName))
                return list.indexOf(u);
        }
        return -1;
    }

    private void assertRoles(List<RoleRepresentation> actual, String... expected) {
        assertThat("Actual and expected size of Roles are different.", actual.size(), is(expected.length));

        for (String e : expected) {
            boolean found = false;
            for (RoleRepresentation r : actual) {
                if (r.getName().equals(e)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Role " + e + " not found");
            }
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

}
