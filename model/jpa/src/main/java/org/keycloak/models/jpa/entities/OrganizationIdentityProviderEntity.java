/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "ORG_IDENTITY_PROVIDER")
@IdClass(OrganizationIdentityProviderEntity.Key.class)
@NamedQueries({
        @NamedQuery(name = "deleteOrgIdpByRealm",
                query = "delete from OrganizationIdentityProviderEntity oip " +
                        "where oip.organization IN (select o from OrganizationEntity o where o.realmId = :realmId)"),
        @NamedQuery(name = "findManagedLinkByIdp",
                query = "select oip from OrganizationIdentityProviderEntity oip " +
                        "where oip.identityProviderId = :idpId and oip.membershipType = 'MANAGED'")
})
public class OrganizationIdentityProviderEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID")
    private OrganizationEntity organization;

    @Id
    @Column(name = "IDENTITY_PROVIDER_ID")
    private String identityProviderId;

    @Column(name = "AUTO_MEMBERSHIP", nullable = false)
    private boolean autoMembership = true;

    @Column(name = "MEMBERSHIP_TYPE")
    private String membershipType = "UNMANAGED";

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public void setIdentityProviderId(String identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public boolean isAutoMembership() {
        return autoMembership;
    }

    public void setAutoMembership(boolean autoMembership) {
        this.autoMembership = autoMembership;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationIdentityProviderEntity that)) return false;
        return Objects.equals(organization, that.organization)
                && Objects.equals(identityProviderId, that.identityProviderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, identityProviderId);
    }

    public static class Key implements Serializable {

        private OrganizationEntity organization;
        private String identityProviderId;

        public Key() {
        }

        public Key(OrganizationEntity organization, String identityProviderId) {
            this.organization = organization;
            this.identityProviderId = identityProviderId;
        }

        public OrganizationEntity getOrganization() {
            return organization;
        }

        public String getIdentityProviderId() {
            return identityProviderId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(organization, key.organization)
                    && Objects.equals(identityProviderId, key.identityProviderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organization, identityProviderId);
        }
    }
}
