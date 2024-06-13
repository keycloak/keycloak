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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.organization.OrganizationProvider;

public class InfinispanOrganizationProvider implements OrganizationProvider {

    private final KeycloakSession session;
    private final OrganizationProvider orgDelegate;
    private final RealmCacheSession realmCache;
    private final Map<String, OrganizationAdapter> managedOrganizations = new HashMap<>();

    public InfinispanOrganizationProvider(KeycloakSession session) {
        this.session = session;
        this.orgDelegate = session.getProvider(OrganizationProvider.class, "jpa");
        this.realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
    }

    static String cacheKeyOrgCount(RealmModel realm) {
        return realm.getId() + ".org.count";
    }

    @Override
    public OrganizationModel create(String name) {
        registerCountInvalidation();
        return orgDelegate.create(name);
    }

    @Override
    public boolean remove(OrganizationModel organization) {
        registerOrganizationInvalidation(organization.getId());
        registerCountInvalidation();
        return orgDelegate.remove(organization);
    }

    @Override
    public OrganizationModel getById(String id) {
        CachedOrganization cached = realmCache.getCache().get(id, CachedOrganization.class);
        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(id);
            OrganizationModel model = orgDelegate.getById(id);
            if (model == null) return null;
            if (realmCache.getInvalidations().contains(id)) return model;
            cached = new CachedOrganization(loaded, getRealm(), model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        // no need to check for realm invalidation as IdP changes are handled by events within InfinispanOrganizationProviderFactory
        } else if (realmCache.getInvalidations().contains(id)) {
            return orgDelegate.getById(id);
        } else if (managedOrganizations.containsKey(id)) {
            return managedOrganizations.get(id);
        }
        OrganizationAdapter adapter = new OrganizationAdapter(cached, realmCache, orgDelegate);
        managedOrganizations.put(id, adapter);
        return adapter;
    }

    @Override
    public OrganizationModel getByDomainName(String domainName) {
        String cacheKey = getRealm().getId() + "+.org.domain.name." + domainName;
        CachedOrganization cached = realmCache.getCache().get(cacheKey, CachedOrganization.class);
        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            OrganizationModel model = orgDelegate.getByDomainName(domainName);
            if (model == null) return null;
            if (realmCache.getInvalidations().contains(model.getId())) return model;
            cached = new CachedOrganization(loaded, getRealm(), model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        // no need to check for realm invalidation as IdP changes are handled by events within InfinispanOrganizationProviderFactory
        } else if (realmCache.getInvalidations().contains(cached.getId())) {
            return orgDelegate.getByDomainName(domainName);
        } else if (managedOrganizations.containsKey(cached.getId())) {
            return managedOrganizations.get(cached.getId());
        }
        OrganizationAdapter adapter = new OrganizationAdapter(cached, realmCache, orgDelegate);
        managedOrganizations.put(cacheKey, adapter);
        return adapter;
    }

    @Override
    public Stream<OrganizationModel> getAllStream(String search, Boolean exact, Integer first, Integer max) {
        // Return cache delegates to ensure cache invalidation during write operations
        return getCacheDelegates(orgDelegate.getAllStream(search, exact, first, max));
    }

    @Override
    public Stream<OrganizationModel> getAllStream(Map<String, String> attributes, Integer first, Integer max) {
        // Return cache delegates to ensure cache invalidation during write operations
        return getCacheDelegates(orgDelegate.getAllStream(attributes, first, max));
    }

    @Override
    public void removeAll() {
        //TODO: won't scale, requires a better mechanism for bulk deleting organizations within a realm
        //this way, all organizations in the realm will be invalidated ... or should it be invalidated whole realm instead?
        getAllStream().forEach(this::remove);
    }

    @Override
    public boolean addMember(OrganizationModel organization, UserModel user) {
        return orgDelegate.addMember(organization, user);
    }

    @Override
    public boolean removeMember(OrganizationModel organization, UserModel member) {
        return orgDelegate.removeMember(organization, member);
    }

    @Override
    public Stream<UserModel> getMembersStream(OrganizationModel organization, String search, Boolean exact, Integer first, Integer max) {
        return orgDelegate.getMembersStream(organization, search, exact, first, max);
    }

    @Override
    public UserModel getMemberById(OrganizationModel organization, String id) {
        return orgDelegate.getMemberById(organization, id);
    }

    @Override
    public OrganizationModel getByMember(UserModel member) {
        return orgDelegate.getByMember(member);
    }

    @Override
    public boolean isManagedMember(OrganizationModel organization, UserModel member) {
        return orgDelegate.isManagedMember(organization, member);
    }

    @Override
    public boolean addIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider) {
        boolean added = orgDelegate.addIdentityProvider(organization, identityProvider);
        if (added) {
            registerOrganizationInvalidation(organization.getId());
        }
        return added;
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProviders(OrganizationModel organization) {
        return orgDelegate.getIdentityProviders(organization);
    }

    @Override
    public boolean removeIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider) {
        boolean removed = orgDelegate.removeIdentityProvider(organization, identityProvider);
        if (removed) {
            registerOrganizationInvalidation(organization.getId());
        }
        return removed;
    }

    @Override
    public boolean isEnabled() {
        return getRealm().isOrganizationsEnabled();
    }

    @Override
    public long count() {
        String cacheKey = cacheKeyOrgCount(getRealm());
        CachedOrganizationCount cached = realmCache.getCache().get(cacheKey, CachedOrganizationCount.class);

        // cached and not invalidated
        if (cached != null && !realmCache.getInvalidations().contains(cacheKey)) {
            return cached.getCount();
        }

        Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
        long count = orgDelegate.count();
        cached = new CachedOrganizationCount(loaded, getRealm(), count);
        realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        return count;
    }

    @Override
    public void close() {
        orgDelegate.close();
    }

    void registerOrganizationInvalidation(String orgId) {
        OrganizationAdapter adapter = managedOrganizations.get(orgId);
        if (adapter != null) {
            adapter.invalidate();
        }

        realmCache.registerInvalidation(orgId);
    }

    private void registerCountInvalidation() {
        realmCache.registerInvalidation(cacheKeyOrgCount(getRealm()));
    }

    private RealmModel getRealm() {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
        return realm;
    }

    private Stream<OrganizationModel> getCacheDelegates(Stream<OrganizationModel> backendOrganizations) {
        return backendOrganizations.map(OrganizationModel::getId).map(this::getById);
    }
}
