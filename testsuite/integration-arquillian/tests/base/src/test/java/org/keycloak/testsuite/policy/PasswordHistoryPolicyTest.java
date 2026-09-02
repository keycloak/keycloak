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
package org.keycloak.testsuite.policy;

import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;

/**
 * @author <a href="mailto:tkyjovsk@redhat.com">Tomas Kyjovsky</a>
 */
public class PasswordHistoryPolicyTest extends AbstractAuthTest {

    UserResource user;

    private void setPasswordHistory(String passwordHistory) {
        log.info(String.format("Setting %s", passwordHistory));
        RealmRepresentation testRealmRepresentation = testRealmResource().toRepresentation();
        testRealmRepresentation.setPasswordPolicy(passwordHistory);
        testRealmResource().update(testRealmRepresentation);
    }
    
    private void setPasswordHistoryValue(String value) {
        setPasswordHistory(String.format("passwordHistory(%s)", value));
    }

    private void setPasswordHistoryValue(int value) {
        setPasswordHistoryValue(String.valueOf(value));
    }

    public UserRepresentation createUserRepresentation(String username) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEmail(String.format("%s@email.test", userRepresentation.getUsername()));
        userRepresentation.setEmailVerified(true);
        return userRepresentation;
    }

    public UserResource createUser(UserRepresentation user) {
        String createdUserId;
        try (Response response = testRealmResource().users().create(user)) {
            createdUserId = getCreatedId(response);
        }
        return testRealmResource().users().get(createdUserId);
    }

    public void resetUserPassword(UserResource userResource, String newPassword) {
        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(PASSWORD);
        newCredential.setValue(newPassword);
        newCredential.setTemporary(false);
        userResource.resetPassword(newCredential);
        CredentialRepresentation cr = userResource.credentials().stream().filter(credentialRepresentation -> credentialRepresentation.getType().equals(PASSWORD)).findFirst().get();
        userResource.setCredentialUserLabel(cr.getId(), "My Password");
    }

    private void expectBadRequestException(Consumer<Void> f) {
        try {
            f.accept(null);
            throw new AssertionError("An expected BadRequestException was not thrown.");
        } catch (BadRequestException bre) {
            log.info("An expected BadRequestException was caught.");
        }
    }

    @Before
    public void before() {
        user = createUser(createUserRepresentation("test_user"));
    }

    @After
    public void after() {
        user.remove();
    }

    @Test
    public void testPasswordHistory_noHistory() {
        setPasswordHistory("");
        resetUserPassword(user, "secret"); // history: 
        resetUserPassword(user, "secret"); // history: 
    }

    @Test
    public void testPasswordHistory_length1() {
        setPasswordHistoryValue(1);
        resetUserPassword(user, "secret"); // history: secret
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret");
        });
        resetUserPassword(user, "secret_2"); // history: secret_2
    }

    @Test
    public void testPasswordHistory_length2() {
        setPasswordHistoryValue(2);
        resetUserPassword(user, "secret"); // history: secret
        resetUserPassword(user, "secret_2"); // history: secret_2, secret
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_2");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret");
        });
        resetUserPassword(user, "secret_3"); // history: secret_3, secret_2
        
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_3");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_2");
        });
        resetUserPassword(user, "secret"); // history: secret, secret_3
    }

    @Test
    public void testPasswordHistory_length3() {
        setPasswordHistoryValue(3);
        resetUserPassword(user, "secret"); // history: secret
        resetUserPassword(user, "secret_2"); // history: secret_2, secret
        resetUserPassword(user, "secret_3"); // history: secret_3, secret_2, secret
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_3");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_2");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret");
        });
        resetUserPassword(user, "secret_4"); // history: secret_4, secret_3, secret_2
        
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_4");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_3");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_2");
        });
        resetUserPassword(user, "secret"); // history: secret, secret_4, secret_3
    }

    @Test
    public void testPasswordHistory_lengthChange() {
        testPasswordHistory_length3(); // history: secret, secret_4, secret_3
        
        setPasswordHistoryValue(2); // history: secret, secret_4
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret");
        });
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_4");
        });
        resetUserPassword(user, "secret_2"); // history: secret_2, secret

        setPasswordHistoryValue(1); // history: secret_2
        expectBadRequestException(f -> {
            resetUserPassword(user, "secret_2");
        });
        resetUserPassword(user, "secret"); // history: secret

        setPasswordHistory(""); // history: 
        resetUserPassword(user, "secret"); // history: 
    }

    @Test
    public void testInvalidPasswordHistoryPolicyValue_notANumber() {
        expectBadRequestException(f -> {
            setPasswordHistoryValue("abc");
        });
        expectBadRequestException(f -> {
            setPasswordHistoryValue("2-");
        });
        expectBadRequestException(f -> {
            setPasswordHistoryValue("-2-");
        });
    }
    
    @Test
    @Ignore("KEYCLOAK-12673")
    public void testInvalidPasswordHistoryPolicyValue_zero() {
        expectBadRequestException(f -> {
            setPasswordHistoryValue(0);
        });
    }

    @Test
    @Ignore("KEYCLOAK-12673")
    public void testInvalidPasswordHistoryPolicyValue_negative() {
        expectBadRequestException(f -> {
            setPasswordHistoryValue(-1);
        });
        expectBadRequestException(f -> {
            setPasswordHistoryValue(-2);
        });
        expectBadRequestException(f -> {
            setPasswordHistoryValue(-10);
        });
    }
    
}
