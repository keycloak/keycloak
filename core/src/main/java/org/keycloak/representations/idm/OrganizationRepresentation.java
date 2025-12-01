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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrganizationRepresentation {

    private String id;
    private String name;
    private String alias;
    private boolean enabled = true;
    private String description;
    private String redirectUrl;
    private Map<String, List<String>> attributes;
    private Set<OrganizationDomainRepresentation> domains;
    private List<MemberRepresentation> members;
    private List<IdentityProviderRepresentation> identityProviders;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>>  attributes) {
        this.attributes = attributes;
    }

    public OrganizationRepresentation singleAttribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<>();
        attributes.put(name, Arrays.asList(value));
        return this;
    }

    public Set<OrganizationDomainRepresentation> getDomains() {
        return domains;
    }

    public OrganizationDomainRepresentation getDomain(String name) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .filter(organizationDomainRepresentation -> name.equals(organizationDomainRepresentation.getName()))
                .findAny()
                .orElse(null);
    }

    public void addDomain(OrganizationDomainRepresentation domain) {
        if (domains == null) {
            domains = new HashSet<>();
        }
        domains.add(domain);
    }

    public void removeDomain(OrganizationDomainRepresentation domain) {
        if (domains == null) {
            return;
        }
        getDomains().remove(domain);
    }

    public List<MemberRepresentation> getMembers() {
        return members;
    }

    public void setMembers(List<MemberRepresentation> members) {
        this.members = members;
    }

    public void addMember(MemberRepresentation member) {
        if (members == null) {
            members = new ArrayList<>();
        }
        members.add(member);
    }

    public List<IdentityProviderRepresentation> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderRepresentation> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public void addIdentityProvider(IdentityProviderRepresentation idp) {
        if (identityProviders == null) {
            identityProviders = new ArrayList<>();
        }
        identityProviders.add(idp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof OrganizationRepresentation)) return false;

        OrganizationRepresentation that = (OrganizationRepresentation) o;

        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return super.hashCode();
        }
        return id.hashCode();
    }
}
