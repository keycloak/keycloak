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
package org.keycloak.testsuite.admin;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class Users {

    public static String getPasswordCredentialValueOf(UserRepresentation user) {
        String value = null;
        CredentialRepresentation password = getPasswordCredentialOf(user);
        if (password != null) {
            value = password.getValue();
        }
        return value;
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

}
