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

package org.keycloak.representations.idm;

/**
 * Representation for organization membership used in partial imports.
 * This allows importing user-to-organization memberships independently from users or organizations.
 */
public class OrganizationMembershipRepresentation {

    private String organizationId;
    private String username;
    private MembershipType membershipType;

    public OrganizationMembershipRepresentation() {
    }

    public OrganizationMembershipRepresentation(String organizationId, String username, MembershipType membershipType) {
        this.organizationId = organizationId;
        this.username = username;
        this.membershipType = membershipType;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrganizationMembershipRepresentation that = (OrganizationMembershipRepresentation) o;

        if (organizationId != null ? !organizationId.equals(that.organizationId) : that.organizationId != null)
            return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return membershipType == that.membershipType;
    }

    @Override
    public int hashCode() {
        int result = organizationId != null ? organizationId.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (membershipType != null ? membershipType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OrganizationMembershipRepresentation{" +
                "organizationId='" + organizationId + '\'' +
                ", username='" + username + '\'' +
                ", membershipType=" + membershipType +
                '}';
    }
}
