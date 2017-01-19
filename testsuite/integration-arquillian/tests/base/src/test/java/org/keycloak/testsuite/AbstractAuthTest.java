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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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

    private static final String TEST_REALM_PRIVATE_KEY_STR = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    private static final String TEST_REALM_PUBLIC_KEY_STR = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealmRep.setPrivateKey(TEST_REALM_PRIVATE_KEY_STR);
        testRealmRep.setPublicKey(TEST_REALM_PUBLIC_KEY_STR);
        testRealms.add(testRealmRep);
    }

    @Before
    public void beforeAuthTest() {
        testRealmLoginPage.setAuthRealm(testRealmPage);
        testRealmAccountPage.setAuthRealm(testRealmPage);

        testUser = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(testUser, PASSWORD);

        bburkeUser = createUserRepresentation("bburke", "bburke@redhat.com", "Bill", "Burke", true);
        setPasswordFor(bburkeUser, PASSWORD);

        deleteAllCookiesForTestRealm();
    }

    public void createTestUserWithAdminClient() {
        log.debug("creating test user");
        String id = createUserAndResetPasswordWithAdminClient(testRealmResource(), testUser, PASSWORD);
        testUser.setId(id);

        assignClientRoles(testRealmResource(), id, "realm-management", "view-realm");
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        return user;
    }

    public void deleteAllCookiesForTestRealm() {
        deleteAllCookiesForRealm(testRealmAccountPage);
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

}