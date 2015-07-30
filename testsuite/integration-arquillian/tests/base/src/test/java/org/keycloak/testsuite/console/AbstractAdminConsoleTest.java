/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.page.auth.AuthRealm;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;
import org.keycloak.testsuite.page.auth.Login;
import static org.keycloak.testsuite.util.LoginAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 * @param <P>
 */
public abstract class AbstractAdminConsoleTest<P extends AdminConsole> extends AbstractKeycloakTest {

    @Page
    protected P page;

    @Page
    protected AuthRealm testAuthRealm;
    @Page
    protected Login testLogin;

    @Page
    protected AdminConsole testAdminConsole;
    @Page
    protected AdminConsoleRealm testAdminConsoleRealm;

    protected RealmResource testRealmResource;
    protected UserRepresentation testUser;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testAuthRealm.setAuthRealm(TEST);
        testLogin.setAuthRealm(TEST);
        testAdminConsole.setAdminRealm(TEST);
        testAdminConsoleRealm.setConsoleRealm(TEST).setAdminRealm(TEST);
    }

    @Before
    public void beforeConsoleTest() {
        testRealmResource = keycloak.realm(TEST);

        createTestUser();

        testAuthRealm.navigateTo();
        driver.manage().deleteAllCookies();
        loginAsTestUser();
    }

    @After
    public void afterConsoleTest() {
//        logoutFromTestRealm();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public void createTestUser() {
        System.out.println("creating test user");

        testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setEmail("test@email.test");
        testUser.setFirstName("test");
        testUser.setLastName("user");
        testUser.setEnabled(true);
        Response response = testRealmResource.users().create(testUser);
        response.close();

        testUser = findUserByUsername(testRealmResource, testUser.getUsername());

        System.out.println(" resetting password");

        UserResource testUserResource = testRealmResource.users().get(testUser.getId());
        CredentialRepresentation testUserPassword = new CredentialRepresentation();
        testUserPassword.setType(PASSWORD);
        testUserPassword.setValue(PASSWORD);
        testUserPassword.setTemporary(false);
        testUserResource.resetPassword(testUserPassword);

        System.out.println(" adding realm-admin role for the test realm");

        ClientRepresentation realmManagementClient = findClientByClientId(testRealmResource, "realm-management");
        RoleScopeResource rsr = testUserResource.roles().clientLevel(realmManagementClient.getId());

        List<RoleRepresentation> realmMgmtRoles = new ArrayList<>();
        for (RoleRepresentation rr : rsr.listAvailable()) {
            if ("realm-admin".equals(rr.getName())) {
                realmMgmtRoles.add(rr);
            }
        }
        rsr.add(realmMgmtRoles);
    }

    public void loginAsTestUser() {
        testAdminConsole.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testAdminConsole);
        testLogin.login(testUser.getUsername(), PASSWORD);
        assertCurrentUrlStartsWith(testAdminConsole);
    }

    public void logoutFromTestRealm() {
        testAdminConsole.navigateTo();
        assertCurrentUrlStartsWith(testAdminConsole);
        menu.logOut();
        assertCurrentUrlStartsWithLoginUrlOf(testAdminConsole);
    }

}
