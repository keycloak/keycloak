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

package org.keycloak.representations.idm;

public class OrganizationUserRepresentation {

    private OrganizationRepresentation organization;
    private UserRepresentation user;

    public OrganizationUserRepresentation(OrganizationRepresentation organization, UserRepresentation user) {
        this.organization = organization;
        this.user = user;
    }

    public OrganizationUserRepresentation() {
    }

    public OrganizationRepresentation getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationRepresentation organization) {
        this.organization = organization;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrganizationUserRepresentation that = (OrganizationUserRepresentation) o;

        if (organization != null ? !organization.equals(that.organization) : that.organization != null){
            return false;
        }
        return user != null ? user.equals(that.user) : that.user == null;
    }

    @Override
    public int hashCode() {
        int result = organization != null ? organization.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
