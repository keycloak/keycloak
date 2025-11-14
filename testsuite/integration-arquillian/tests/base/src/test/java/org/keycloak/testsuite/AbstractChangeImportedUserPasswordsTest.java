/*
 * Copyright 2025 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.testsuite;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import org.junit.Before;

/**
 * <p>Abstract class that re-generates all imported user passwords with a random one.
 * This way all the passwords are random and cannot be rejected by security
 * configurations in browsers (chrome for example). The passwords are stored in
 * a map inside the test context to be retrieved using the username.</p>
 *
 * @author rmartinc
 */
abstract public class AbstractChangeImportedUserPasswordsTest extends AbstractTestRealmKeycloakTest {

    private Map<String, String> userPasswords = null;

    @Before
    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        if (userPasswords == null) {
            userPasswords = testContext.getUserPasswords();
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (userPasswords == null) {
            userPasswords = testContext.getUserPasswords();
        }
        userPasswords.clear();
        List<UserRepresentation> users = testRealm.getUsers();
        for (UserRepresentation user : users) {
            List<CredentialRepresentation> credentials = user.getCredentials();
            if (credentials != null) {
                for (CredentialRepresentation cred : credentials) {
                    if (CredentialRepresentation.PASSWORD.equals(cred.getType())) {
                        // re-generate the password for the user using a random one
                        cred.setValue(generatePassword(user.getUsername()));
                    }
                }
            }
        }
        testContext.setUserPasswords(userPasswords);
    }

    protected String changePassword(String username) {
        UserResource userRes = ApiUtil.findUserByUsernameId(testRealm(), username);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PasswordCredentialModel.TYPE);
        credential.setTemporary(Boolean.FALSE);
        credential.setValue(generatePassword());
        userRes.resetPassword(credential);
        userPasswords.put(username, credential.getValue());
        return credential.getValue();
    }

    protected void changePasswords(String... usernames) {
        if (usernames != null) {
            for (String username: usernames) {
                changePassword(username);
            }
        }
    }

    protected void generatePasswords(String... usernames) {
        if (usernames != null) {
            for (String username: usernames) {
                generatePassword(username);
            }
        }
    }

    protected String generatePassword(String username) {
        final String password = generatePassword();
        userPasswords.put(username, password);
        return password;
    }

    protected String getPassword(String username) {
        final String password = userPasswords.get(username);
        Assert.assertNotNull("password not generated for user " + username, password);
        return password;
    }
}
