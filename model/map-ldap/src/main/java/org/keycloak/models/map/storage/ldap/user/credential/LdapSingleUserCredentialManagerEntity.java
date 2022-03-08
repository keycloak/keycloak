/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.user.credential;

import org.keycloak.credential.CredentialInput;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.map.credential.MapSubjectCredentialManagerEntity;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.user.LdapUserMapKeycloakTransaction;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Adapter to forward calls to the Map storage API to LDAP.
 *
 * @author Alexander Schwartz
 */
public class LdapSingleUserCredentialManagerEntity implements MapSubjectCredentialManagerEntity {
    private final LdapUserMapKeycloakTransaction transaction;
    private final LdapMapObject user;

    public LdapSingleUserCredentialManagerEntity(LdapUserMapKeycloakTransaction transaction, LdapMapObject user) {
        this.transaction = transaction;
        this.user = user;
    }

    @Override
    public void validateCredentials(List<CredentialInput> inputs) {
        inputs.removeIf(input -> {
            try {
                if (input instanceof UserCredentialModel && input.getType().equals(PasswordCredentialModel.TYPE)) {
                    transaction.getIdentityStore().validatePassword(user, input.getChallengeResponse());
                    return true;
                }
            } catch(AuthenticationException ex) {
                return false;
            }
            return false;
        });
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        if (input instanceof UserCredentialModel && input.getType().equals(PasswordCredentialModel.TYPE)) {
            transaction.getIdentityStore().updatePassword(user, ((UserCredentialModel) input).getValue(), null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return false;
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        return Stream.empty();
    }
}
