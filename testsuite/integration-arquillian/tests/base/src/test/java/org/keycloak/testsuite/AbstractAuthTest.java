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
package org.keycloak.testsuite;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.SAMLPostLogin;
import org.keycloak.testsuite.auth.page.login.SAMLRedirectLogin;
import org.openqa.selenium.Cookie;

import java.text.MessageFormat;
import java.util.List;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.assignClientRoles;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAuthTest extends AbstractKeycloakTest {

    @Page
    protected AuthRealm testRealmPage;
    @Page
    protected OIDCLogin testRealmLoginPage;
    @Page
    protected Account testRealmAccountPage;

    @Page
    protected SAMLPostLogin testRealmSAMLPostLoginPage;

    @Page
    protected SAMLRedirectLogin testRealmSAMLRedirectLoginPage;

    protected UserRepresentation testUser;

    protected UserRepresentation bburkeUser;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeAuthTest() {
        testRealmLoginPage.setAuthRealm(testRealmPage);
        testRealmAccountPage.setAuthRealm(testRealmPage);

        testUser = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(testUser, PASSWORD);

        bburkeUser = createUserRepresentation("bburke", "bburke@redhat.com", "Bill", "Burke", true);
        setPasswordFor(bburkeUser, PASSWORD);

        resetTestRealmSession();
    }


    public void createTestUserWithAdminClient() {
        createTestUserWithAdminClient(true);
    }

    public void createTestUserWithAdminClient(boolean setRealmRoles) {
        ApiUtil.removeUserByUsername(testRealmResource(), "test");

        log.debug("creating test user");
        String id = createUserAndResetPasswordWithAdminClient(testRealmResource(), testUser, PASSWORD);
        testUser.setId(id);

        if (setRealmRoles) {
            assignClientRoles(testRealmResource(), id, "realm-management", "view-realm");
        }
    }

    protected void deleteAllCookiesForTestRealm() {
        deleteAllCookiesForRealm(testRealmAccountPage);
    }

    protected void deleteAllSessionsInTestRealm() {
        deleteAllSessionsInRealm(testRealmAccountPage.getAuthRealm());
    }

    protected void resetTestRealmSession() {
        resetRealmSession(testRealmAccountPage.getAuthRealm());
    }

    public void listCookies() {
        log.info("LIST OF COOKIES: ");
        for (Cookie c : driver.manage().getCookies()) {
            log.info(MessageFormat.format(" {1} {2} {0}",
                    c.getName(), c.getDomain(), c.getPath(), c.getValue()));
        }
    }

    public RealmResource testRealmResource() {
        return adminClient.realm(testRealmPage.getAuthRealm());
    }

    protected UserResource testUserResource() {
        return testRealmResource().users().get(testUser.getId());
    }

}
