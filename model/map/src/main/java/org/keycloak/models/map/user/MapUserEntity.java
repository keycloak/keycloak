/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author mhajas
 */
public class MapUserEntity<K> implements AbstractEntity<K> {

    private final K id;
    private final String realmId;

    private String username;
    private String firstName;
    private Long createdTimestamp;
    private String lastName;
    private String email;
    private boolean enabled;
    private boolean emailVerified;
    // This is necessary to be able to dynamically switch unique email constraints on and off in the realm settings
    private String emailConstraint = KeycloakModelUtils.generateId();
    private Map<String, List<String>> attributes = new HashMap<>();
    private Set<String> requiredActions = new HashSet<>();
    private final Map<String, UserCredentialEntity> credentials = new HashMap<>();
    private final List<String> credentialsOrder = new LinkedList<>();
    private final Map<String, UserFederatedIdentityEntity> federatedIdentities = new HashMap<>();
    private final Map<String, UserConsentEntity> userConsents = new HashMap<>();
    private Set<String> groupsMembership = new HashSet<>();
    private Set<String> rolesMembership = new HashSet<>();
    private String federationLink;
    private String serviceAccountClientLink;
    private int notBefore;

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    protected MapUserEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapUserEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated
                || userConsents.values().stream().anyMatch(UserConsentEntity::isUpdated)
                || credentials.values().stream().anyMatch(UserCredentialEntity::isUpdated)
                || federatedIdentities.values().stream().anyMatch(UserFederatedIdentityEntity::isUpdated);
    }

    public String getRealmId() {
        return realmId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.updated |= !Objects.equals(this.username, username);
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.updated |= !Objects.equals(this.firstName, firstName);
        this.firstName = firstName;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.updated |= !Objects.equals(this.createdTimestamp, createdTimestamp);
        this.createdTimestamp = createdTimestamp;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.updated |= !Objects.equals(this.lastName, lastName);
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email, boolean duplicateEmailsAllowed) {
        this.updated |= !Objects.equals(this.email, email);
        this.email = email;
        this.emailConstraint = email == null || duplicateEmailsAllowed ? KeycloakModelUtils.generateId() : email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.updated |= !Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.updated |= !Objects.equals(this.emailVerified, emailVerified);
        this.emailVerified = emailVerified;
    }

    public String getEmailConstraint() {
        return emailConstraint;
    }

    public void setEmailConstraint(String emailConstraint) {
        this.updated |= !Objects.equals(this.emailConstraint, emailConstraint);
        this.emailConstraint = emailConstraint;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public List<String> getAttribute(String name) {
        return attributes.getOrDefault(name, Collections.emptyList());
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.updated |= !Objects.equals(this.attributes, attributes);
        this.attributes = attributes;
    }

    public void setAttribute(String name, List<String> value) {
        this.updated |= !Objects.equals(this.attributes.put(name, value), value);
    }
    
    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<String> requiredActions) {
        this.updated |= !Objects.equals(this.requiredActions, requiredActions);
        this.requiredActions = requiredActions;
    }

    public void addRequiredAction(String requiredAction) {
        this.updated |= this.requiredActions.add(requiredAction);
    }

    public void removeRequiredAction(String requiredAction) {
        this.updated |= this.requiredActions.remove(requiredAction);
    }

    public void updateCredential(UserCredentialEntity credentialEntity) {
        this.updated |= credentials.replace(credentialEntity.getId(), credentialEntity) != null;
    }

    public void addCredential(UserCredentialEntity credentialEntity) {
        if (credentials.containsKey(credentialEntity.getId())) {
            throw new ModelDuplicateException("A CredentialModel with given id already exists");
        }

        this.updated = true;
        credentials.put(credentialEntity.getId(), credentialEntity);
        credentialsOrder.add(credentialEntity.getId());
    }
    
    public boolean removeCredential(String credentialId) {
        if (!credentials.containsKey(credentialId)) {
            return false;
        }

        this.updated = true;
        this.credentials.remove(credentialId);
        this.credentialsOrder.remove(credentialId);

        return true;
    }
    
    public UserCredentialEntity getCredential(String id) {
        return credentials.get(id);
    }
    
    public Stream<UserCredentialEntity> getCredentials() {
        return credentialsOrder.stream()
                .map(credentials::get);
    }
    
    public int getCredentialIndex(String credentialId) {
        return credentialsOrder.indexOf(credentialId);
    }
    
    public void moveCredential(int currentPosition, int newPosition) {
        this.updated |= currentPosition != newPosition;
        credentialsOrder.add(newPosition, credentialsOrder.remove(currentPosition));
    }

    public Stream<UserFederatedIdentityEntity> getFederatedIdentities() {
        return federatedIdentities.values().stream();
    }

    public void setFederatedIdentities(Collection<UserFederatedIdentityEntity> federatedIdentities) {
        this.updated = true;
        this.federatedIdentities.clear();
        this.federatedIdentities.putAll(federatedIdentities.stream()
                .collect(Collectors.toMap(UserFederatedIdentityEntity::getIdentityProvider, Function.identity())));
    }
    
    public void addFederatedIdentity(UserFederatedIdentityEntity federatedIdentity) {
        String idpId = federatedIdentity.getIdentityProvider();
        this.updated |= !Objects.equals(this.federatedIdentities.put(idpId, federatedIdentity), federatedIdentity);
    }

    public UserFederatedIdentityEntity getFederatedIdentity(String federatedIdentity) {
        return this.federatedIdentities.get(federatedIdentity);
    }
    
    public boolean removeFederatedIdentity(String providerId) {
        boolean removed = federatedIdentities.remove(providerId) != null;
        this.updated |= removed;
        return removed;
    }

    public void updateFederatedIdentity(UserFederatedIdentityEntity federatedIdentityModel) {
        this.updated |= federatedIdentities.replace(federatedIdentityModel.getIdentityProvider(), federatedIdentityModel) != null;
    }

    public Stream<UserConsentEntity> getUserConsents() {
        return userConsents.values().stream();
    }

    public UserConsentEntity getUserConsent(String clientId) {
        return this.userConsents.get(clientId);
    }

    
    public void addUserConsent(UserConsentEntity userConsentEntity) {
        String clientId = userConsentEntity.getClientId();
        this.updated |= !Objects.equals(this.userConsents.put(clientId, userConsentEntity), userConsentEntity);
    }

    public boolean removeUserConsent(String clientId) {
        boolean removed = userConsents.remove(clientId) != null;
        this.updated |= removed;
        return removed;
    }

    public Set<String> getGroupsMembership() {
        return groupsMembership;
    }

    public void setGroupsMembership(Set<String> groupsMembership) {
        this.updated |= Objects.equals(groupsMembership, this.groupsMembership);
        this.groupsMembership = groupsMembership;
    }
    
    public void addGroupsMembership(String groupId) {
        this.updated |= this.groupsMembership.add(groupId);
    }

    public void removeGroupsMembership(String groupId) {
        this.updated |= this.groupsMembership.remove(groupId);
    }

    public Set<String> getRolesMembership() {
        return rolesMembership;
    }

    public void setRolesMembership(Set<String> rolesMembership) {
        this.updated |= Objects.equals(rolesMembership, this.rolesMembership);
        this.rolesMembership = rolesMembership;
    }

    public void addRolesMembership(String roleId) {
        this.updated |= this.rolesMembership.add(roleId);
    }

    public void removeRolesMembership(String roleId) {
        this.updated |= this.rolesMembership.remove(roleId);
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.updated |= !Objects.equals(this.federationLink, federationLink);
        this.federationLink = federationLink;
    }

    public String getServiceAccountClientLink() {
        return serviceAccountClientLink;
    }

    public void setServiceAccountClientLink(String serviceAccountClientLink) {
        this.updated |= !Objects.equals(this.serviceAccountClientLink, serviceAccountClientLink);
        this.serviceAccountClientLink = serviceAccountClientLink;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.updated |= !Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

}
