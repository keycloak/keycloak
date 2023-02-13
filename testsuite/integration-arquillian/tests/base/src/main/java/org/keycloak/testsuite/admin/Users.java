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
package org.keycloak.testsuite.admin;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class Users {

    public static String getPasswordOf(UserRepresentation user) {
        CredentialRepresentation credential = getPasswordCredentialOf(user);
        if (credential == null) {
            return null;
        }
        if (credential.getValue() != null && !credential.getValue().isEmpty()) {
            return credential.getValue();
        }
        return credential.getSecretData();
    }

    public static CredentialRepresentation getPasswordCredentialOf(UserRepresentation user) {
        CredentialRepresentation password = null;
        if (user.getCredentials() != null) {
            for (CredentialRepresentation c : user.getCredentials()) {
                if (CredentialRepresentation.PASSWORD.equals(c.getType())) {
                    password = c;
                }
            }
        }
        return password;
    }

    public static void setPasswordFor(UserRepresentation user, String password) {
        setPasswordFor(user, password, false);
    }
    public static void setPasswordFor(UserRepresentation user, String password, boolean temporary) {
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation pass = new CredentialRepresentation();
        pass.setType(PASSWORD);
        pass.setValue(password);
        pass.setTemporary(temporary);
        credentials.add(pass);
        user.setCredentials(credentials);
    }

}
