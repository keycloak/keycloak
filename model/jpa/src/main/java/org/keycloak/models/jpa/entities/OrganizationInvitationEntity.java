/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.common.util.Time;
import org.keycloak.models.OrganizationInvitationModel;

/**
 * JPA entity for organization invitations.
 */
@Entity
@Table(name = "ORG_INVITATION")
@NamedQueries({
        @NamedQuery(name="getByOrganization", query="SELECT i FROM OrganizationInvitationEntity i WHERE i.organizationId = :orgId ORDER BY i.createdAt DESC")
})
public class OrganizationInvitationEntity implements OrganizationInvitationModel {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "ORGANIZATION_ID", length = 255, nullable = false)
    private String organizationId;

    @Column(name = "EMAIL", nullable = false)
    private String email;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "CREATED_AT", nullable = false)
    private int createdAt;

    @Column(name = "EXPIRES_AT")
    private int expiresAt;

    @Column(name = "INVITE_LINK", length = 2048)
    private String inviteLink;

    public OrganizationInvitationEntity() {
    }

    public OrganizationInvitationEntity(String id, String organizationId, String email,
                                        String firstName, String lastName) {
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = Time.currentTime();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(int createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int getExpiresAt() {
        return expiresAt;
    }

    @Override
    public void setExpiresAt(int expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String getInviteLink() {
        return inviteLink;
    }

    @Override
    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }

    @Override
    public InvitationStatus getStatus() {
        return isExpired() ? InvitationStatus.EXPIRED : InvitationStatus.PENDING;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrganizationInvitationEntity that = (OrganizationInvitationEntity) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
