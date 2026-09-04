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

package org.keycloak.models;

import java.io.Serializable;
import java.util.Objects;

import org.keycloak.representations.idm.MembershipType;

public class OrganizationIdentityProviderLinkModel implements Serializable {

    private final String identityProviderId;
    private final boolean autoMembership;
    private final MembershipType membershipType;

    public OrganizationIdentityProviderLinkModel(String identityProviderId, boolean autoMembership, MembershipType membershipType) {
        this.identityProviderId = identityProviderId;
        this.autoMembership = autoMembership;
        this.membershipType = membershipType;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public boolean isAutoMembership() {
        return autoMembership;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof OrganizationIdentityProviderLinkModel)) return false;

        OrganizationIdentityProviderLinkModel that = (OrganizationIdentityProviderLinkModel) o;
        return identityProviderId != null && identityProviderId.equals(that.getIdentityProviderId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identityProviderId);
    }
}
