/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.user.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.user.MapUserConsentEntity;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserFederatedIdentityEntity;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;

/**
 * JPA {@link MapUserEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"realmId", "username"}),
                @UniqueConstraint(columnNames = {"realmId", "emailConstraint"})
        })
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
@SuppressWarnings("ConstantConditions")
public class JpaUserEntity extends MapUserEntity.AbstractUserEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaUserMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String realmId;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String username;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String firstName;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String lastName;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String email;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String emailConstraint;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String federationLink;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Boolean enabled;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Boolean emailVerified;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long timestamp;

    @Column(name = "group_id")
    @ElementCollection
    @CollectionTable(name = "kc_user_group", joinColumns = @JoinColumn(name = "user_id", nullable = false))
    private final Set<String> groupIds = new HashSet<>();

    @Column(name = "role_id")
    @ElementCollection
    @CollectionTable(name = "kc_user_role", joinColumns = @JoinColumn(name = "user_id", nullable = false))
    private final Set<String> roleIds = new HashSet<>();

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaUserAttributeEntity> attributes = new HashSet<>();

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaUserConsentEntity> consents = new HashSet<>();

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaUserFederatedIdentityEntity> federatedIdentities = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaUserEntity() {
        this.metadata = new JpaUserMetadata();
    }

    public JpaUserEntity(final DeepCloner cloner) {
        this.metadata = new JpaUserMetadata(cloner);
    }

    /**
     * Used by hibernate when calling cb.construct from read(QueryParameters) method.
     * It is used to select user without metadata(json) field.
     */
    public JpaUserEntity(final UUID id, final int version, final Integer entityVersion, final String realmId, final String username,
                         final String firstName, final String lastName, final String email, final String emailConstraint,
                         final String federationLink, final Boolean enabled, final Boolean emailVerified, final Long timestamp) {
        this.id = id;
        this.version = version;
        this.entityVersion = entityVersion;
        this.realmId = realmId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.emailConstraint = emailConstraint;
        this.federationLink = federationLink;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.timestamp = timestamp;
        this.metadata = null;
    }

    public boolean isMetadataInitialized() {
        return this.metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (isMetadataInitialized()) return this.metadata.getEntityVersion();
        return this.entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.metadata.setEntityVersion(entityVersion);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_USER;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public String getId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getRealmId() {
        if (this.isMetadataInitialized()) return this.metadata.getRealmId();
        return this.realmId;
    }

    @Override
    public void setRealmId(String realmId) {
        this.metadata.setRealmId(realmId);
    }

    @Override
    public String getUsername() {
        if (this.isMetadataInitialized()) return this.metadata.getUsername();
        return this.username;
    }

    @Override
    public void setUsername(String username) {
        this.metadata.setUsername(username);
    }

    @Override
    public String getFirstName() {
        if (this.isMetadataInitialized()) return this.metadata.getFirstName();
        return this.firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.metadata.setFirstName(firstName);
    }

    @Override
    public Long getCreatedTimestamp() {
        if (this.isMetadataInitialized()) return this.metadata.getCreatedTimestamp();
        return this.timestamp;
    }

    @Override
    public void setCreatedTimestamp(Long createdTimestamp) {
        this.metadata.setCreatedTimestamp(createdTimestamp);
    }

    @Override
    public String getLastName() {
        if (this.isMetadataInitialized()) return this.metadata.getLastName();
        return this.lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.metadata.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        if (this.isMetadataInitialized()) return this.metadata.getEmail();
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.metadata.setEmail(email);
    }

    @Override
    public Boolean isEnabled() {
        if (this.isMetadataInitialized()) return this.metadata.isEnabled();
        return this.enabled;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.metadata.setEnabled(enabled);
    }

    @Override
    public Boolean isEmailVerified() {
        if (this.isMetadataInitialized()) return this.metadata.isEmailVerified();
        return this.emailVerified;
    }

    @Override
    public void setEmailVerified(Boolean emailVerified) {
        this.metadata.setEmailVerified(emailVerified);
    }

    @Override
    public String getEmailConstraint() {
        if (this.isMetadataInitialized()) return this.metadata.getEmailConstraint();
        return this.emailConstraint;
    }

    @Override
    public void setEmailConstraint(String emailConstraint) {
        this.metadata.setEmailConstraint(emailConstraint);
    }

    @Override
    public String getFederationLink() {
        if (this.isMetadataInitialized()) return this.metadata.getFederationLink();
        return this.federationLink;
    }

    @Override
    public void setFederationLink(String federationLink) {
        this.metadata.setFederationLink(federationLink);
    }

    @Override
    public String getServiceAccountClientLink() {
        return this.metadata.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String serviceAccountClientLink) {
        this.metadata.setServiceAccountClientLink(serviceAccountClientLink);
    }

    @Override
    public Long getNotBefore() {
        return this.metadata.getNotBefore();
    }

    @Override
    public void setNotBefore(Long notBefore) {
        this.metadata.setNotBefore(notBefore);
    }

    //groups membership
    @Override
    public Set<String> getGroupsMembership() {
        return this.groupIds;
    }

    @Override
    public void setGroupsMembership(Set<String> groupsMembership) {
        this.groupIds.clear();
        if (groupsMembership != null) this.groupIds.addAll(groupsMembership);
    }

    @Override
    public void addGroupsMembership(String groupId) {
        this.groupIds.add(groupId);
    }

    @Override
    public void removeGroupsMembership(String groupId) {
        this.groupIds.remove(groupId);
    }

    //roles membership
    @Override
    public Set<String> getRolesMembership() {
        return this.roleIds;
    }

    @Override
    public void setRolesMembership(Set<String> rolesMembership) {
        this.roleIds.clear();
        if (rolesMembership != null) this.roleIds.addAll(rolesMembership);
    }

    @Override
    public void addRolesMembership(String roleId) {
        this.roleIds.add(roleId);
    }

    @Override
    public void removeRolesMembership(String roleId) {
        this.roleIds.remove(roleId);
    }

    //user required actions
    @Override
    public Set<String> getRequiredActions() {
        return this.metadata.getRequiredActions();
    }

    @Override
    public void setRequiredActions(Set<String> requiredActions) {
        this.metadata.setRequiredActions(requiredActions);
    }

    @Override
    public void addRequiredAction(String requiredAction) {
        this.metadata.addRequiredAction(requiredAction);
    }

    @Override
    public void removeRequiredAction(String requiredAction) {
        this.metadata.removeRequiredAction(requiredAction);
    }

    //user attributes
    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        for (JpaUserAttributeEntity attribute : this.attributes) {
            List<String> values = result.getOrDefault(attribute.getName(), new LinkedList<>());
            values.add(attribute.getValue());
            result.put(attribute.getName(), values);
        }
        return result;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            attributes.forEach(this::setAttribute);
        }
    }

    @Override
    public List<String> getAttribute(String name) {
        return this.attributes.stream().filter(a -> Objects.equals(a.getName(), name))
                .map(JpaUserAttributeEntity::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        this.removeAttribute(name);
        if (values != null) {
            values.forEach(value -> this.attributes.add(new JpaUserAttributeEntity(this, name, value)));
        }
    }

    @Override
    public void removeAttribute(String name) {
        this.attributes.removeIf(attr -> Objects.equals(attr.getName(), name));
    }

    //user consents
    @Override
    public Set<MapUserConsentEntity> getUserConsents() {
        return this.consents.stream().map(MapUserConsentEntity.class::cast).collect(Collectors.toSet());
    }

    @Override
    public void setUserConsents(Set<MapUserConsentEntity> userConsents) {
        this.consents.clear();
        if (userConsents != null) {
            userConsents.forEach(this::addUserConsent);
        }
    }

    @Override
    public void addUserConsent(MapUserConsentEntity userConsentEntity) {
        JpaUserConsentEntity entity = (JpaUserConsentEntity) CLONER.from(userConsentEntity);
        entity.setParent(this);
        entity.setEntityVersion(this.getEntityVersion());
        this.consents.add(entity);
    }

    @Override
    public Boolean removeUserConsent(MapUserConsentEntity userConsentEntity) {
        return this.consents.removeIf(uc -> Objects.equals(uc.getClientId(), userConsentEntity.getClientId()));
    }

    @Override
    public Boolean removeUserConsent(String clientId) {
        return this.consents.removeIf(uc -> Objects.equals(uc.getClientId(), clientId));
    }

    //user credentials
    @Override
    public List<MapUserCredentialEntity> getCredentials() {
        return this.metadata.getCredentials();
    }

    @Override
    public void setCredentials(List<MapUserCredentialEntity> credentials) {
        this.metadata.setCredentials(credentials);
    }

    @Override
    public void addCredential(MapUserCredentialEntity credentialEntity) {
        this.metadata.addCredential(credentialEntity);
    }

    @Override
    public Boolean removeCredential(MapUserCredentialEntity credentialEntity) {
        return super.removeCredential(credentialEntity.getId());
    }

    //user federated identities
    @Override
    public Set<MapUserFederatedIdentityEntity> getFederatedIdentities() {
        return this.federatedIdentities.stream().map(MapUserFederatedIdentityEntity.class::cast).collect(Collectors.toSet());
    }

    @Override
    public void setFederatedIdentities(Set<MapUserFederatedIdentityEntity> federatedIdentities) {
        this.federatedIdentities.clear();
        if (federatedIdentities != null) {
            federatedIdentities.forEach(this::addFederatedIdentity);
        }
    }

    @Override
    public void addFederatedIdentity(MapUserFederatedIdentityEntity federatedIdentity) {
        JpaUserFederatedIdentityEntity entity = (JpaUserFederatedIdentityEntity) CLONER.from(federatedIdentity);
        entity.setParent(this);
        entity.setEntityVersion(this.getEntityVersion());
        this.federatedIdentities.add(entity);
    }

    @Override
    public Boolean removeFederatedIdentity(MapUserFederatedIdentityEntity federatedIdentity) {
        return this.federatedIdentities.removeIf(fi -> Objects.equals(fi.getIdentityProvider(), federatedIdentity.getIdentityProvider()));
    }

    @Override
    public Boolean removeFederatedIdentity(String identityProviderId) {
        return this.federatedIdentities.removeIf(fi -> Objects.equals(fi.getIdentityProvider(), identityProviderId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaUserEntity)) return false;
        return Objects.equals(getId(), ((JpaUserEntity) obj).getId());
    }
}
