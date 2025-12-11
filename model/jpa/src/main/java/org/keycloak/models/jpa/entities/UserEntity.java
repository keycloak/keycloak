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

package org.keycloak.models.jpa.entities;

import java.util.Collection;
import java.util.LinkedList;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.keycloak.models.utils.KeycloakModelUtils;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Nationalized;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getRealmUserByUsername", query="select u from UserEntity u where u.username = :username and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByEmail", query="select u from UserEntity u where u.email = :email and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByLastName", query="select u from UserEntity u where u.lastName = :lastName and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByFirstLastName", query="select u from UserEntity u where u.firstName = :first and u.lastName = :last and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByServiceAccount", query="select u from UserEntity u where u.serviceAccountClientLink = :clientInternalId and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUsersByAttributeNameAndValue", query="select u from UserEntity u join u.attributes attr " +
                "where u.realmId = :realmId and attr.name = :name and attr.value = :value"),
        @NamedQuery(name="getRealmUsersByAttributeNameAndLongValue", query="select u from UserEntity u join u.attributes attr " +
                "where u.realmId = :realmId and attr.name = :name and attr.longValueHash = :longValueHash"),
        @NamedQuery(name="deleteUsersByRealm", query="delete from UserEntity u where u.realmId = :realmId"),
        @NamedQuery(name="deleteUsersByRealmAndLink", query="delete from UserEntity u where u.realmId = :realmId and u.federationLink=:link"),
        @NamedQuery(name="unlinkUsers", query="update UserEntity u set u.federationLink = null where u.realmId = :realmId and u.federationLink=:link")
})
@Entity
@Table(name="USER_ENTITY", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "REALM_ID", "USERNAME" }),
        @UniqueConstraint(columnNames = { "REALM_ID", "EMAIL_CONSTRAINT" })
})
public class UserEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Nationalized
    @Column(name = "USERNAME")
    protected String username;
    @Nationalized
    @Column(name = "FIRST_NAME")
    protected String firstName;
    @Column(name = "CREATED_TIMESTAMP")
    protected Long createdTimestamp;
    @Nationalized
    @Column(name = "LAST_NAME")
    protected String lastName;
    @Column(name = "EMAIL")
    protected String email;
    @Column(name = "ENABLED")
    protected boolean enabled;
    @Column(name = "EMAIL_VERIFIED")
    protected boolean emailVerified;

    // This is necessary to be able to dynamically switch unique email constraints on and off in the realm settings
    @Column(name = "EMAIL_CONSTRAINT")
    protected String emailConstraint = KeycloakModelUtils.generateId();

    @Column(name = "REALM_ID")
    protected String realmId;

    // Explicitly not using OrphanRemoval as we're handling the removal manually through HQL but at the same time we still
    // want to remove elements from the entity's collection in a manual way. Without this, Hibernate would do a duplicit
    // delete query.
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = false, mappedBy="user")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected Collection<UserAttributeEntity> attributes = new LinkedList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected Collection<UserRequiredActionEntity> requiredActions = new LinkedList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected Collection<CredentialEntity> credentials = new LinkedList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected Collection<FederatedIdentityEntity> federatedIdentities = new LinkedList<>();

    @Column(name="FEDERATION_LINK")
    protected String federationLink;

    @Column(name="SERVICE_ACCOUNT_CLIENT_LINK")
    protected String serviceAccountClientLink;

    @Column(name="NOT_BEFORE")
    protected int notBefore;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long timestamp) {
        createdTimestamp = timestamp;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email, boolean allowDuplicate) {
        this.email = email;
        this.emailConstraint = email == null || allowDuplicate ? KeycloakModelUtils.generateId() : email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmailConstraint() {
        return emailConstraint;
    }

    public void setEmailConstraint(String emailConstraint) {
        this.emailConstraint = emailConstraint;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Collection<UserAttributeEntity> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedList<>();
        }
        return attributes;
    }

    public void setAttributes(Collection<UserAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    public Collection<UserRequiredActionEntity> getRequiredActions() {
        if (requiredActions == null) {
            requiredActions = new LinkedList<>();
        }
        return requiredActions;
    }

    public void setRequiredActions(Collection<UserRequiredActionEntity> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Collection<CredentialEntity> getCredentials() {
        if (credentials == null) {
            credentials = new LinkedList<>();
        }
        return credentials;
    }

    public void setCredentials(Collection<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public Collection<FederatedIdentityEntity> getFederatedIdentities() {
        if (federatedIdentities == null) {
            federatedIdentities = new LinkedList<>();
        }
        return federatedIdentities;
    }

    public void setFederatedIdentities(Collection<FederatedIdentityEntity> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }

    public String getServiceAccountClientLink() {
        return serviceAccountClientLink;
    }

    public void setServiceAccountClientLink(String serviceAccountClientLink) {
        this.serviceAccountClientLink = serviceAccountClientLink;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserEntity)) return false;

        UserEntity that = (UserEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
