/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.user.MapUserEntity.AbstractUserEntity"
)
@DeepCloner.Root
public interface MapUserEntity extends UpdatableEntity, AbstractEntity, EntityWithAttributes {

    public abstract class AbstractUserEntity extends UpdatableEntity.Impl implements MapUserEntity {

        private String id;

        @Override
        public boolean isUpdated() {
            return this.updated
                    || Optional.ofNullable(getUserConsents()).orElseGet(Collections::emptyMap).values().stream().anyMatch(MapUserConsentEntity::isUpdated)
                    || Optional.ofNullable(getCredentials()).orElseGet(Collections::emptyMap).values().stream().anyMatch(MapUserCredentialEntity::isUpdated)
                    || Optional.ofNullable(getFederatedIdentities()).orElseGet(Collections::emptyMap).values().stream().anyMatch(MapUserFederatedIdentityEntity::isUpdated);
        }

        @Override
        public void clearUpdatedFlag() {
            this.updated = false;
            Optional.ofNullable(getUserConsents()).orElseGet(Collections::emptyMap).values().forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getCredentials()).orElseGet(Collections::emptyMap).values().forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getFederatedIdentities()).orElseGet(Collections::emptyMap).values().forEach(UpdatableEntity::clearUpdatedFlag);

        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void setId(String id) {
            if (this.id != null) throw new IllegalStateException("Id cannot be changed");
            this.id = id;
            this.updated |= id != null;
        }

        @Override
        public void setEmail(String email, boolean duplicateEmailsAllowed) {
            this.setEmail(email);
            this.setEmailConstraint(email == null || duplicateEmailsAllowed ? KeycloakModelUtils.generateId() : email);
        }
    }

    String getRealmId();
    void setRealmId(String realmId);

    String getUsername();
    void setUsername(String username);

    String getFirstName();
    void setFirstName(String firstName);

    Long getCreatedTimestamp();
    void setCreatedTimestamp(Long createdTimestamp);

    String getLastName();
    void setLastName(String lastName);

    String getEmail();
    void setEmail(String email);
    @IgnoreForEntityImplementationGenerator
    void setEmail(String email, boolean duplicateEmailsAllowed);

    Boolean isEnabled();
    void setEnabled(Boolean enabled);

    Boolean isEmailVerified();
    void setEmailVerified(Boolean emailVerified);

    String getEmailConstraint();
    void setEmailConstraint(String emailConstraint);

    Map<String, List<String>> getAttributes();
    List<String> getAttribute(String name);
    void setAttributes(Map<String, List<String>> attributes);
    void setAttribute(String name, List<String> value);
    void removeAttribute(String name);

    Set<String> getRequiredActions();
    void setRequiredActions(Set<String> requiredActions);
    void addRequiredAction(String requiredAction);
    void removeRequiredAction(String requiredAction);

    Map<String, MapUserCredentialEntity> getCredentials();
    void setCredential(String id, MapUserCredentialEntity credentialEntity);
    Boolean removeCredential(String credentialId);
    MapUserCredentialEntity getCredential(String id);

    Map<String, MapUserFederatedIdentityEntity> getFederatedIdentities();
    void setFederatedIdentities(Map<String, MapUserFederatedIdentityEntity> federatedIdentities);
    void setFederatedIdentity(String id, MapUserFederatedIdentityEntity federatedIdentity);
    MapUserFederatedIdentityEntity getFederatedIdentity(String federatedIdentity);
    Boolean removeFederatedIdentity(String providerId);

    Map<String, MapUserConsentEntity> getUserConsents();
    MapUserConsentEntity getUserConsent(String clientId);
    void setUserConsent(String id, MapUserConsentEntity userConsentEntity);
    Boolean removeUserConsent(String clientId);

    Set<String> getGroupsMembership();
    void setGroupsMembership(Set<String> groupsMembership);
    void addGroupsMembership(String groupId);
    void removeGroupsMembership(String groupId);

    Set<String> getRolesMembership();
    void setRolesMembership(Set<String> rolesMembership);
    void addRolesMembership(String roleId);
    void removeRolesMembership(String roleId);

    String getFederationLink();
    void setFederationLink(String federationLink);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String serviceAccountClientLink);

    Integer getNotBefore();
    void setNotBefore(Integer notBefore);
}
