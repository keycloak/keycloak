/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.storage;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.BackwardsCompatibilityUserStorageFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * Test that userStorage implementation created in previous version is still compatible with latest Keycloak version
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BackwardsCompatibilityUserStorageTest extends AbstractAuthTest {

    private String backwardsCompProviderId;

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("backwards-compatibility");
        memProvider.setProviderId(BackwardsCompatibilityUserStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));

        backwardsCompProviderId = addComponent(memProvider);

    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = testRealmResource().components().add(component);
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }


    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    private void loginSuccessAndLogout(String username, String password) {
        testRealmAccountPage.navigateTo();
        loginPage.login(username, password);
        assertCurrentUrlStartsWith(testRealmAccountPage);
        testRealmAccountPage.logOut();
    }

    public void loginBadPassword(String username) {
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(username, "badpassword");
        assertCurrentUrlDoesntStartWith(testRealmAccountPage);
    }

    @Test
    public void testLoginSuccess() {
        addUserAndResetPassword("tbrady", "goat");
        addUserAndResetPassword("tbrady2", "goat2");

        loginSuccessAndLogout("tbrady", "goat");
        loginSuccessAndLogout("tbrady2", "goat2");
        loginBadPassword("tbrady");
    }

    private void addUserAndResetPassword(String username, String password) {
        // Save user and assert he is saved in the new storage
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        Response response = testRealmResource().users().create(user);
        String userId = ApiUtil.getCreatedId(response);

        Assert.assertEquals(backwardsCompProviderId, new StorageId(userId).getProviderId());

        // Update his password
        CredentialRepresentation passwordRep = new CredentialRepresentation();
        passwordRep.setType(CredentialModel.PASSWORD);
        passwordRep.setValue(password);
        passwordRep.setTemporary(false);

        testRealmResource().users().get(userId).resetPassword(passwordRep);
    }
}