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
package org.keycloak.models.cache.infinispan.organization;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.InRealm;

public class CachedOrganization extends AbstractRevisioned implements InRealm {

    private final String realm;
    private final String name;
    private final String alias;
    private final String description;
    private final String redirectUrl;
    private final boolean enabled;
    private final LazyLoader<OrganizationModel, MultivaluedHashMap<String, String>> attributes;
    private final Set<OrganizationDomainModel> domains;
    private final Set<IdentityProviderModel> idps;

    public CachedOrganization(Long revision, RealmModel realm, OrganizationModel organization) {
        super(revision, organization.getId());
        this.realm = realm.getId();
        this.name = organization.getName();
        this.alias = organization.getAlias();
        this.description = organization.getDescription();
        this.redirectUrl = organization.getRedirectUrl();
        this.enabled = organization.isEnabled();
        this.attributes = new DefaultLazyLoader<>(orgModel -> new MultivaluedHashMap<>(orgModel.getAttributes()), MultivaluedHashMap::new);
        this.domains = organization.getDomains().collect(Collectors.toSet());
        this.idps = organization.getIdentityProviders().collect(Collectors.toSet());
    }

    @Override
    public String getRealm() {
        return realm;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getDescription() {
        return description;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MultivaluedHashMap<String, String> getAttributes(KeycloakSession session, Supplier<OrganizationModel> organizationModel) {
        return attributes.get(session, organizationModel);
    }

    public Stream<OrganizationDomainModel> getDomains() {
        return domains.stream();
    }

    public Stream<IdentityProviderModel> getIdentityProviders() {
        return idps.stream();
    }
}
