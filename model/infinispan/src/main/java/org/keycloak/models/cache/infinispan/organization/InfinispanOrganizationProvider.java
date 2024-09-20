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
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.CachedCount;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.organization.OrganizationProvider;

import static org.keycloak.models.cache.infinispan.idp.InfinispanIdentityProviderStorageProvider.cacheKeyOrgId;

public class InfinispanOrganizationProvider implements OrganizationProvider {

    private static final String ORG_COUNT_KEY_SUFFIX = ".org.count";
    private static final String ORG_MEMBERS_COUNT_KEY_SUFFIX = ".members.count";

    private final KeycloakSession session;
    private final OrganizationProvider orgDelegate;
    private final RealmCacheSession realmCache;
    private final Map<String, OrganizationAdapter> managedOrganizations = new HashMap<>();

    public InfinispanOrganizationProvider(KeycloakSession session) {
        this.session = session;
        this.orgDelegate = session.getProvider(OrganizationProvider.class, "jpa");
        this.realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
    }

    private static String cacheKeyOrgCount(RealmModel realm) {
        return realm.getId() + ORG_COUNT_KEY_SUFFIX;
    }

    public static String cacheKeyOrgMemberCount(RealmModel realm, OrganizationModel organization) {
        return realm.getId() + ".org." + organization.getId() + ORG_MEMBERS_COUNT_KEY_SUFFIX;
    }

    @Override
    public OrganizationModel create(String name, String alias) {
        registerCountInvalidation();
        return orgDelegate.create(name, alias);
    }

    @Override
    public boolean remove(OrganizationModel organization) {
        registerOrganizationInvalidation(organization);
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
            if (isInvalid(id)) return model;
            cached = new CachedOrganization(loaded, getRealm(), model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        // no need to check for realm invalidation as IdP changes are handled by events within InfinispanOrganizationProviderFactory
        } else if (isInvalid(id)) {
            return orgDelegate.getById(id);
        } else if (managedOrganizations.containsKey(id)) {
            return managedOrganizations.get(id);
        }
        OrganizationAdapter adapter = new OrganizationAdapter(cached, realmCache, orgDelegate, this);
        managedOrganizations.put(id, adapter);
        return adapter;
    }

    @Override
    public OrganizationModel getByDomainName(String domainName) {
        String cacheKey = cacheKeyByDomain(domainName);

        if (isInvalid(cacheKey)) {
            return orgDelegate.getByDomainName(domainName);
        }

        CachedOrganizationIds cached = realmCache.getCache().get(cacheKey, CachedOrganizationIds.class);

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            OrganizationModel model = orgDelegate.getByDomainName(domainName);
            if (model == null) {
                return null;
            }
            cached = new CachedOrganizationIds(loaded, cacheKey, getRealm(), Stream.of(model));
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return cached.getOrgIds().stream().map(this::getById).findAny().orElse(null);
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
    public boolean addManagedMember(OrganizationModel organization, UserModel user) {
        registerMemberInvalidation(organization, user);
        return orgDelegate.addManagedMember(organization, user);
    }

    @Override
    public boolean addMember(OrganizationModel organization, UserModel user) {
        registerMemberInvalidation(organization, user);
        return orgDelegate.addMember(organization, user);
    }

    @Override
    public boolean removeMember(OrganizationModel organization, UserModel member) {
        registerMemberInvalidation(organization, member);
        return orgDelegate.removeMember(organization, member);
    }

    @Override
    public Stream<UserModel> getMembersStream(OrganizationModel organization, String search, Boolean exact, Integer first, Integer max) {
        return orgDelegate.getMembersStream(organization, search, exact, first, max);
    }

    @Override
    public long getMembersCount(OrganizationModel organization) {
        String cacheKey = cacheKeyOrgMemberCount(getRealm(), organization);
        CachedCount cached = realmCache.getCache().get(cacheKey, CachedCount.class);

        // cached and not invalidated
        if (cached != null && !isInvalid(cacheKey)) {
            return cached.getCount();
        }

        Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
        long membersCount = orgDelegate.getMembersCount(organization);
        cached = new CachedCount(loaded, getRealm(), cacheKey, membersCount);
        realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        return membersCount;
    }

    @Override
    public UserModel getMemberById(OrganizationModel organization, String id) {
        RealmModel realm = getRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            return null;
        }

        String cacheKey = cacheKeyMembership(realm, organization, user);

        if (isInvalid(cacheKey)) {
            return orgDelegate.getMemberById(organization, user.getId());
        }

        CachedMembership cached = realmCache.getCache().get(cacheKey, CachedMembership.class);

        if (cached == null) {
            boolean isManaged = orgDelegate.isManagedMember(organization, user);
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            UserModel member = orgDelegate.getMemberById(organization, user.getId());
            cached = new CachedMembership(loaded, cacheKeyMembership(realm, organization, user), realm, isManaged, member != null);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return cached.isMember() ? user : null;
    }

    @Override
    public Stream<OrganizationModel> getByMember(UserModel member) {
        String cacheKey = cacheKeyByMember(member);

        if (isInvalid(cacheKey)) {
            return orgDelegate.getByMember(member);
        }

        CachedOrganizationIds cached = realmCache.getCache().get(cacheKey, CachedOrganizationIds.class);

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            Stream<OrganizationModel> model = orgDelegate.getByMember(member);
            cached = new CachedOrganizationIds(loaded, cacheKey, getRealm(), model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return cached.getOrgIds().stream().map(this::getById);
    }

    @Override
    public boolean isManagedMember(OrganizationModel organization, UserModel user) {
        UserModel member = getMemberById(organization, user.getId());

        if (member == null) {
            return false;
        }

        String cacheKey = cacheKeyMembership(getRealm(), organization, member);
        CachedMembership cached = realmCache.getCache().get(cacheKey, CachedMembership.class);

        if (cached == null) {
            return orgDelegate.isManagedMember(organization, user);
        }

        return cached.isManaged();

    }

    @Override
    public boolean addIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider) {
        boolean added = orgDelegate.addIdentityProvider(organization, identityProvider);
        if (added) {
            registerOrganizationInvalidation(organization);
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
            registerOrganizationInvalidation(organization);
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
        CachedCount cached = realmCache.getCache().get(cacheKey, CachedCount.class);

        // cached and not invalidated
        if (cached != null && !isInvalid(cacheKey)) {
            return cached.getCount();
        }

        Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
        long count = orgDelegate.count();
        cached = new CachedCount(loaded, getRealm(), cacheKey, count);
        realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        return count;
    }

    @Override
    public void close() {
        orgDelegate.close();
    }

    void registerOrganizationInvalidation(OrganizationModel organization) {
        String id = organization.getId();

        realmCache.registerInvalidation(cacheKeyOrgId(getRealm(), id));
        realmCache.registerInvalidation(id);
        organization.getDomains()
                .map(OrganizationDomainModel::getName)
                .map(this::cacheKeyByDomain)
                .forEach(realmCache::registerInvalidation);

        OrganizationAdapter adapter = managedOrganizations.get(id);

        if (adapter != null) {
            adapter.invalidate();
        }
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

    private String cacheKeyByDomain(String domainName) {
        return getRealm().getId() + ".org.domain.name." + domainName;
    }

    private String cacheKeyByMember(UserModel user) {
        return getRealm().getId() + ".org.member." + user.getId() + ".orgs";
    }

    private String cacheKeyMembership(RealmModel realm, OrganizationModel organization, UserModel user) {
        return realm.getId() + ".org." + organization.getId() + ".member." + user.getId() + ".membership";
    }

    void registerMemberInvalidation(OrganizationModel organization, UserModel member) {
        realmCache.registerInvalidation(cacheKeyByMember(member));
        realmCache.registerInvalidation(cacheKeyMembership(getRealm(), organization, member));
        realmCache.registerInvalidation(cacheKeyOrgMemberCount(getRealm(), organization));
    }

    private boolean isInvalid(String cacheKey) {
        return realmCache.getInvalidations().contains(cacheKey);
    }
}
