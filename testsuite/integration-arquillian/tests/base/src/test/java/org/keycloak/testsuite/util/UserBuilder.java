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

package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserBuilder {

    private final UserRepresentation rep;

    public static UserBuilder create() {
        UserRepresentation rep = new UserRepresentation();
        rep.setEnabled(Boolean.TRUE);
        return new UserBuilder(rep);
    }

    public static UserBuilder edit(UserRepresentation rep) {
        return new UserBuilder(rep);
    }

    private UserBuilder(UserRepresentation rep) {
        this.rep = rep;
    }

    public UserBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public UserBuilder username(String username) {
        rep.setUsername(username);
        return this;
    }

    public UserBuilder firstName(String firstName) {
        rep.setFirstName(firstName);
        return this;
    }

    public UserBuilder lastName(String lastName) {
        rep.setLastName(lastName);
        return this;
    }

    /**
     * This method adds additional passwords to the user.
     */
    public UserBuilder addPassword(String password) {
        if (rep.getCredentials() == null) {
            rep.setCredentials(new LinkedList<CredentialRepresentation>());
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);

        rep.getCredentials().add(credential);
        return this;
    }

    /**
     * This method makes sure that there is one single password for the user.
     */
    public UserBuilder password(String password) {
        rep.setCredentials(null);
        return addPassword(password);
    }

    public UserBuilder email(String email) {
        rep.setEmail(email);
        return this;
    }

    public UserBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public UserBuilder addRoles(String... roles) {
        if (rep.getRealmRoles() == null) {
            rep.setRealmRoles(new ArrayList<String>());
        }
        for (String role : roles) {
            rep.getRealmRoles().add(role);
        }
        return this;
    }

    public UserBuilder role(String client, String role) {
        if (rep.getClientRoles() == null) {
            rep.setClientRoles(new HashMap<String, List<String>>());
        }
        if (rep.getClientRoles().get(client) == null) {
            rep.getClientRoles().put(client, new LinkedList<String>());
        }
        rep.getClientRoles().get(client).add(role);
        return this;
    }

    public UserBuilder requiredAction(String requiredAction) {
        if (rep.getRequiredActions() == null) {
            rep.setRequiredActions(new LinkedList<String>());
        }
        rep.getRequiredActions().add(requiredAction);
        return this;
    }

    public UserBuilder serviceAccountId(String serviceAccountId) {
        rep.setServiceAccountClientId(serviceAccountId);
        return this;
    }

    public UserBuilder secret(String type, String secret) {
        if (rep.getCredentials() == null) {
            rep.setCredentials(new LinkedList<CredentialRepresentation>());
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(type);
        credential.setValue(secret);

        rep.getCredentials().add(credential);
        rep.setTotp(true);
        return this;
    }

    public UserBuilder totpSecret(String totpSecret) {
        return secret(CredentialRepresentation.TOTP, totpSecret);
    }

    public UserBuilder hotpSecret(String hotpSecret) {
        return secret(CredentialRepresentation.HOTP, hotpSecret);
    }

    public UserBuilder otpEnabled() {
        rep.setTotp(Boolean.TRUE);
        return this;
    }

    public UserRepresentation build() {
        return rep;
    }

}
