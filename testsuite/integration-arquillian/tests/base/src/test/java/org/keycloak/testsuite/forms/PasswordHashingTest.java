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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.UserBuilder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.ws.rs.BadRequestException;
import java.security.spec.KeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class PasswordHashingTest extends AbstractTestRealmKeycloakTest {

    @Page
    private AccountUpdateProfilePage updateProfilePage;

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

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        loginPage.open();
        loginPage.login(username, "password");

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    public void testPasswordRehashedOnIterationsChanged() throws Exception {
        setPasswordPolicy("hashIterations(10000)");

        String username = "testPasswordRehashedOnIterationsChanged";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(10000, credential.getPasswordCredentialData().getHashIterations());

        setPasswordPolicy("hashIterations(1)");

        loginPage.open();
        loginPage.login(username, "password");

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(1, credential.getPasswordCredentialData().getHashIterations());
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);
    }

    // KEYCLOAK-5282
    @Test
    public void testPasswordNotRehasedUnchangedIterations() {
        setPasswordPolicy("");

        String username = "testPasswordNotRehasedUnchangedIterations";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        String credentialId = credential.getId();
        byte[] salt = credential.getPasswordSecretData().getSalt();

        setPasswordPolicy("hashIterations");

        loginPage.open();
        loginPage.login(username, "password");

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());

        setPasswordPolicy("hashIterations(" + Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        updateProfilePage.open();
        updateProfilePage.logout();

        loginPage.open();
        loginPage.login(username, "password");

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());
    }

    @Test
    public void testPasswordRehashedWhenCredentialImportedWithDifferentKeySize() {
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
        UserRepresentation user = UserBuilder.create().username(username).password(encodedPassword).build();
        ApiUtil.createUserWithAdminClient(adminClient.realm("test"),user);

        loginPage.open();
        loginPage.login(username, password);

        PasswordCredentialModel postLoginCredentials = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEquals(encodedPassword.length() * 2, postLoginCredentials.getPasswordSecretData().getValue().length());

    }


    @Test
    public void testPbkdf2Sha1() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha1";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 20000);
    }

    @Test
    public void testDefault() throws Exception {
        setPasswordPolicy("");
        String username = "testDefault";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 27500);
    }

    @Test
    public void testPbkdf2Sha256() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha256";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 27500);
    }

    @Test
    public void testPbkdf2Sha512() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha512";
        createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, "password", credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", 30000);
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

    private void assertEncoded(PasswordCredentialModel credential, String password, byte[] salt, String algorithm, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 512);
        byte[] key = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        assertEquals(Base64.encodeBytes(key), credential.getPasswordSecretData().getValue());
    }

}
