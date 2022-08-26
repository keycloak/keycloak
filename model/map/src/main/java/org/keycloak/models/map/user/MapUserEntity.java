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

import org.jboss.logging.Logger;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.credential.DefaultMapSubjectCredentialManagerEntity;
import org.keycloak.models.map.credential.MapSubjectCredentialManagerEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.user.MapUserEntity.AbstractUserEntity"
)
@DeepCloner.Root
public interface MapUserEntity extends UpdatableEntity, AbstractEntity, EntityWithAttributes {

    abstract class AbstractUserEntity extends Impl implements MapUserEntity {

        private static final Logger LOG = Logger.getLogger(MapUserProvider.class);
        private String id;

        @Override
        public boolean isUpdated() {
            return this.updated
                    || Optional.ofNullable(getUserConsents()).orElseGet(Collections::emptySet).stream().anyMatch(MapUserConsentEntity::isUpdated)
                    || Optional.ofNullable(getCredentials()).orElseGet(Collections::emptyList).stream().anyMatch(MapUserCredentialEntity::isUpdated)
                    || Optional.ofNullable(getFederatedIdentities()).orElseGet(Collections::emptySet).stream().anyMatch(MapUserFederatedIdentityEntity::isUpdated);
        }

        @Override
        public void clearUpdatedFlag() {
            this.updated = false;
            Optional.ofNullable(getUserConsents()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getCredentials()).orElseGet(Collections::emptyList).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getFederatedIdentities()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);

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

        @Override
        public Optional<MapUserConsentEntity> getUserConsent(String clientId) {
            Set<MapUserConsentEntity> ucs = getUserConsents();
            if (ucs == null || ucs.isEmpty()) return Optional.empty();

            return ucs.stream().filter(uc -> Objects.equals(uc.getClientId(), clientId)).findFirst();
        }

        @Override
        public Boolean removeUserConsent(String clientId) {
            Set<MapUserConsentEntity> consents = getUserConsents();
            boolean removed = consents != null && consents.removeIf(uc -> Objects.equals(uc.getClientId(), clientId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapUserCredentialEntity> getCredential(String id) {
            List<MapUserCredentialEntity> uce = getCredentials();
            if (uce == null || uce.isEmpty()) return Optional.empty();

            return uce.stream().filter(uc -> Objects.equals(uc.getId(), id)).findFirst();
        }

        @Override
        public Boolean removeCredential(String id) {
            List<MapUserCredentialEntity> credentials = getCredentials();
            boolean removed = credentials != null && credentials.removeIf(c -> Objects.equals(c.getId(), id));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Boolean moveCredential(String credentialId, String newPreviousCredentialId) {
            // 1 - Get all credentials from the entity.
            List<MapUserCredentialEntity> credentialsList = getCredentials();

            // 2 - Find indexes of our and newPrevious credential
            int ourCredentialIndex = -1;
            int newPreviousCredentialIndex = -1;
            MapUserCredentialEntity ourCredential = null;
            int i = 0;
            for (MapUserCredentialEntity credential : credentialsList) {
                if (credentialId.equals(credential.getId())) {
                    ourCredentialIndex = i;
                    ourCredential = credential;
                } else if(newPreviousCredentialId != null && newPreviousCredentialId.equals(credential.getId())) {
                    newPreviousCredentialIndex = i;
                }
                i++;
            }

            if (ourCredentialIndex == -1) {
                LOG.warnf("Not found credential with id [%s] of user [%s]", credentialId, getUsername());
                return false;
            }

            if (newPreviousCredentialId != null && newPreviousCredentialIndex == -1) {
                LOG.warnf("Can't move up credential with id [%s] of user [%s]", credentialId, getUsername());
                return false;
            }

            // 3 - Compute index where we move our credential
            int toMoveIndex = newPreviousCredentialId==null ? 0 : newPreviousCredentialIndex + 1;

            // 4 - Insert our credential to new position, remove it from the old position
            if (toMoveIndex == ourCredentialIndex) return true;
            credentialsList.add(toMoveIndex, ourCredential);
            int indexToRemove = toMoveIndex < ourCredentialIndex ? ourCredentialIndex + 1 : ourCredentialIndex;
            credentialsList.remove(indexToRemove);

            this.updated = true;
            return true;
        }

        @Override
        public Optional<MapUserFederatedIdentityEntity> getFederatedIdentity(String identityProviderId) {
            Set<MapUserFederatedIdentityEntity> fes = getFederatedIdentities();
            if (fes == null || fes.isEmpty()) return Optional.empty();

            return fes.stream().filter(fi -> Objects.equals(fi.getIdentityProvider(), identityProviderId)).findFirst();
        }

        @Override
        public Boolean removeFederatedIdentity(String identityProviderId) {
            Set<MapUserFederatedIdentityEntity> federatedIdentities = getFederatedIdentities();
            boolean removed = federatedIdentities != null && federatedIdentities.removeIf(fi -> Objects.equals(fi.getIdentityProvider(), identityProviderId));
            this.updated |= removed;
            return removed;
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

    Set<String> getRequiredActions();
    void setRequiredActions(Set<String> requiredActions);
    void addRequiredAction(String requiredAction);
    void removeRequiredAction(String requiredAction);

    List<MapUserCredentialEntity> getCredentials();
    Optional<MapUserCredentialEntity> getCredential(String id);
    void setCredentials(List<MapUserCredentialEntity> credentials);
    void addCredential(MapUserCredentialEntity credentialEntity);
    Boolean removeCredential(MapUserCredentialEntity credentialEntity);
    Boolean removeCredential(String id);
    @IgnoreForEntityImplementationGenerator
    Boolean moveCredential(String credentialId, String newPreviousCredentialId);

    Set<MapUserFederatedIdentityEntity> getFederatedIdentities();
    Optional<MapUserFederatedIdentityEntity> getFederatedIdentity(String identityProviderId);
    void setFederatedIdentities(Set<MapUserFederatedIdentityEntity> federatedIdentities);
    void addFederatedIdentity(MapUserFederatedIdentityEntity federatedIdentity);
    Boolean removeFederatedIdentity(MapUserFederatedIdentityEntity providerId);
    Boolean removeFederatedIdentity(String identityProviderId);

    Set<MapUserConsentEntity> getUserConsents();
    Optional<MapUserConsentEntity> getUserConsent(String clientId);
    void setUserConsents(Set<MapUserConsentEntity> userConsentEntity);
    void addUserConsent(MapUserConsentEntity userConsentEntity);
    Boolean removeUserConsent(MapUserConsentEntity userConsentEntity);
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

    Long getNotBefore();
    void setNotBefore(Long notBefore);

    default MapSubjectCredentialManagerEntity credentialManager() {
        return new DefaultMapSubjectCredentialManagerEntity();
    }
}
