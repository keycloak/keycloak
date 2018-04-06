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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordHashingTest extends AbstractTestRealmKeycloakTest {

    @Page
    private AccountUpdateProfilePage updateProfilePage;

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(PasswordHashingTest.class, AbstractTestRealmKeycloakTest.class);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Page
    protected LoginPage loginPage;

    @Test
    public void testSetInvalidProvider() throws Exception {
        try {
            setPasswordPolicy("hashAlgorithm(nosuch)");
            fail("Expected error");
        } catch (BadRequestException e) {
            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Invalid config for hashAlgorithm: Password hashing provider not found", error.getErrorMessage());
        }
    }

    @Test
    public void testPasswordRehashedOnAlgorithmChanged() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String username = "testPasswordRehashedOnAlgorithmChanged";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getAlgorithm());

        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        loginPage.open();
        loginPage.login(username, "password");

        credential = fetchCredentials(username);

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getAlgorithm());
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    public void testPasswordRehashedOnIterationsChanged() throws Exception {
        setPasswordPolicy("hashIterations(10000)");

        String username = "testPasswordRehashedOnIterationsChanged";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);

        assertEquals(10000, credential.getHashIterations());

        setPasswordPolicy("hashIterations(1)");

        loginPage.open();
        loginPage.login(username, "password");

        credential = fetchCredentials(username);

        assertEquals(1, credential.getHashIterations());
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA256", 1);
    }

    // KEYCLOAK-5282
    @Test
    public void testPasswordNotRehasedUnchangedIterations() throws Exception {
        setPasswordPolicy("");

        String username = "testPasswordNotRehasedUnchangedIterations";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);
        String credentialId = credential.getId();
        byte[] salt = credential.getSalt();

        setPasswordPolicy("hashIterations");

        loginPage.open();
        loginPage.login(username, "password");

        credential = fetchCredentials(username);

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getSalt());

        setPasswordPolicy("hashIterations(" + Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        updateProfilePage.open();
        updateProfilePage.logout();

        loginPage.open();
        loginPage.login(username, "password");

        credential = fetchCredentials(username);

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getSalt());
    }

    @Test
    public void testPasswordRehashedWhenCredentialImportedWithDifferentKeySize() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ") and hashIterations("+ Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        String username = "testPasswordRehashedWhenCredentialImportedWithDifferentKeySize";
        String password = "password";

        // Encode with a specific key size ( 256 instead of default: 512)
        Pbkdf2PasswordHashProvider specificKeySizeHashProvider = new Pbkdf2PasswordHashProvider(Pbkdf2Sha512PasswordHashProviderFactory.ID,
                Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM,
                Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS,
                256);
        String encodedPassword = specificKeySizeHashProvider.encode(password, -1);

        // Create a user with the encoded password, simulating a user import from a different system using a specific key size
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setAlgorithm(Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM);
        credentialRepresentation.setHashedSaltedValue(encodedPassword);
        UserRepresentation user = UserBuilder.create().username(username).password(encodedPassword).build();
        ApiUtil.createUserWithAdminClient(adminClient.realm("test"),user);

        loginPage.open();
        loginPage.login(username, password);

        CredentialModel postLoginCredentials = fetchCredentials(username);
        assertEquals(encodedPassword.length() * 2, postLoginCredentials.getValue().length());

    }


    @Test
    public void testPbkdf2Sha1() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha1";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA1", 20000);
    }

    @Test
    public void testDefault() throws Exception {
        setPasswordPolicy("");
        String username = "testDefault";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA256", 27500);
    }

    @Test
    public void testPbkdf2Sha256() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha256";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA256", 27500);
    }

    @Test
    public void testPbkdf2Sha512() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha512";
        createUser(username);

        CredentialModel credential = fetchCredentials(username);
        assertEncoded(credential, "password", credential.getSalt(), "PBKDF2WithHmacSHA512", 30000);
    }


    private void createUser(String username) {
        ApiUtil.createUserAndResetPasswordWithAdminClient(adminClient.realm("test"), UserBuilder.create().username(username).build(), "password");
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        testRealm().update(realmRep);
    }

    private CredentialModel fetchCredentials(String username) {
        return testingClient.server("test").fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(username, realm);
            return session.userCredentialManager().getStoredCredentialsByType(realm, user, CredentialRepresentation.PASSWORD).get(0);
        }, CredentialModel.class);
    }

    private void assertEncoded(CredentialModel credential, String password, byte[] salt, String algorithm, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 512);
        byte[] key = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        assertEquals(Base64.encodeBytes(key), credential.getValue());
    }

}
