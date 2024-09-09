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
package org.keycloak.models.cache.infinispan.idp;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.CachedCount;
import org.keycloak.models.cache.infinispan.RealmCacheManager;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.organization.OrganizationProvider;

public class InfinispanIdentityProviderStorageProvider implements IdentityProviderStorageProvider {

    private static final String IDP_COUNT_KEY_SUFFIX = ".idp.count";
    private static final String IDP_ALIAS_KEY_SUFFIX = ".idp.alias";
    private static final String IDP_ORG_ID_KEY_SUFFIX = ".idp.orgId";

    private final KeycloakSession session;
    private final IdentityProviderStorageProvider idpDelegate;
    private final RealmCacheSession realmCache;
    private final long startupRevision;

    public InfinispanIdentityProviderStorageProvider(KeycloakSession session) {
        this.session = session;
        this.idpDelegate = session.getProvider(IdentityProviderStorageProvider.class, "jpa");
        this.realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
        this.startupRevision = realmCache.getCache().getCurrentCounter();
    }

    private static String cacheKeyIdpCount(RealmModel realm) {
        return realm.getId() + IDP_COUNT_KEY_SUFFIX;
    }

    private static String cacheKeyIdpAlias(RealmModel realm, String alias) {
        return realm.getId() + "." + alias + IDP_ALIAS_KEY_SUFFIX;
    }

    private static String cacheKeyIdpMapperAliasName(RealmModel realm, String alias, String name) {
        return realm.getId() + "." + alias + IDP_ALIAS_KEY_SUFFIX + "." + name;
    }

    public static String cacheKeyOrgId(RealmModel realm, String orgId) {
        return realm.getId() + "." + orgId + IDP_ORG_ID_KEY_SUFFIX;
    }

    @Override
    public IdentityProviderModel create(IdentityProviderModel model) {
        registerCountInvalidation();
        return idpDelegate.create(model);
    }

    @Override
    public void update(IdentityProviderModel model) {
        // for cases the alias is being updated, it is needed to lookup the idp by id to obtain the original alias
        IdentityProviderModel idpById = getById(model.getInternalId());
        registerIDPInvalidation(idpById);
        idpDelegate.update(model);
    }

    @Override
    public boolean remove(String alias) {
        String cacheKey = cacheKeyIdpAlias(getRealm(), alias);
        if (isInvalid(cacheKey)) {
            //lookup idp by alias in cache to be able to invalidate its internalId
            registerIDPInvalidation(idpDelegate.getByAlias(alias));
        } else {
            CachedIdentityProvider cached = realmCache.getCache().get(cacheKey, CachedIdentityProvider.class);
            if (cached != null) {
                registerIDPInvalidation(cached.getIdentityProvider());
            }
        }
        registerCountInvalidation();
        return idpDelegate.remove(alias);
    }

    @Override
    public void removeAll() {
        registerCountInvalidation();
        // no need to invalidate each entry in cache, removeAll() is (currently) called only in case the realm is being deleted
        idpDelegate.removeAll();
    }

    @Override
    public IdentityProviderModel getById(String internalId) {
        if (internalId == null) return null;
        CachedIdentityProvider cached = realmCache.getCache().get(internalId, CachedIdentityProvider.class);
        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(internalId);
            IdentityProviderModel model = idpDelegate.getById(internalId);
            if (model == null) return null;
            if (isInvalid(internalId)) return createOrganizationAwareIdentityProviderModel(model);
            cached = new CachedIdentityProvider(loaded, getRealm(), internalId, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        } else if (isInvalid(internalId)) {
            return createOrganizationAwareIdentityProviderModel(idpDelegate.getById(internalId));
        }
        return createOrganizationAwareIdentityProviderModel(cached.getIdentityProvider());
    }

    @Override
    public IdentityProviderModel getByAlias(String alias) {
        String cacheKey = cacheKeyIdpAlias(getRealm(), alias);

        if (isInvalid(cacheKey)) {
            return createOrganizationAwareIdentityProviderModel(idpDelegate.getByAlias(alias));
        }

        CachedIdentityProvider cached = realmCache.getCache().get(cacheKey, CachedIdentityProvider.class);

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            IdentityProviderModel model = idpDelegate.getByAlias(alias);
            if (model == null) {
                return null;
            }
            cached = new CachedIdentityProvider(loaded, getRealm(), cacheKey, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return createOrganizationAwareIdentityProviderModel(cached.getIdentityProvider());
    }

    @Override
    public Stream<IdentityProviderModel> getByOrganization(String orgId, Integer first, Integer max) {
        RealmModel realm = getRealm();
        String cacheKey = cacheKeyOrgId(realm, orgId);

        // check if there is invalidation for this key or the organization was invalidated
        if (isInvalid(cacheKey) || isInvalid(orgId)) {
            return idpDelegate.getByOrganization(orgId, first, max).map(this::createOrganizationAwareIdentityProviderModel);
        }

        RealmCacheManager cache = realmCache.getCache();
        IdentityProviderListQuery query = cache.get(cacheKey, IdentityProviderListQuery.class);
        String searchKey = Optional.ofNullable(first).orElse(-1) + "." + Optional.ofNullable(max).orElse(-1);
        Set<String> cached;

        if (query == null) {
            // not cached yet
            Long loaded = cache.getCurrentRevision(cacheKey);
            cached = idpDelegate.getByOrganization(orgId, first, max).map(IdentityProviderModel::getInternalId).collect(Collectors.toSet());
            query = new IdentityProviderListQuery(loaded, cacheKey, realm, searchKey, cached);
            cache.addRevisioned(query, startupRevision);
        } else {
            cached = query.getIDPs(searchKey);
            if (cached == null) {
                // there is a cache entry, but the current search is not yet cached
                cache.invalidateObject(cacheKey);
                Long loaded = cache.getCurrentRevision(cacheKey);
                cached = idpDelegate.getByOrganization(orgId, first, max).map(IdentityProviderModel::getInternalId).collect(Collectors.toSet());
                query = new IdentityProviderListQuery(loaded, cacheKey, realm, searchKey, cached, query);
                cache.addRevisioned(query, cache.getCurrentCounter());
            }
        }

        Set<IdentityProviderModel> identityProviders = new HashSet<>();
        for (String id : cached) {
            IdentityProviderModel idp = session.identityProviders().getById(id);
            if (idp == null) {
                realmCache.registerInvalidation(cacheKey);
                return idpDelegate.getByOrganization(orgId, first, max).map(this::createOrganizationAwareIdentityProviderModel);
            }
            identityProviders.add(idp);
        }

        return identityProviders.stream();
    }

    @Override
    public Stream<String> getByFlow(String flowId, String search, Integer first, Integer max) {
        return idpDelegate.getByFlow(flowId, search, first, max);
    }

    @Override
    public Stream<IdentityProviderModel> getAllStream(Map<String, String> attrs, Integer first, Integer max) {
        return idpDelegate.getAllStream(attrs, first, max).map(this::createOrganizationAwareIdentityProviderModel);
    }

    @Override
    public long count() {
        String cacheKey = cacheKeyIdpCount(getRealm());
        CachedCount cached = realmCache.getCache().get(cacheKey, CachedCount.class);

        // cached and not invalidated
        if (cached != null && !isInvalid(cacheKey)) {
            return cached.getCount();
        }

        Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
        long count = idpDelegate.count();
        cached = new CachedCount(loaded, getRealm(), cacheKey, count);
        realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        return count;
    }

    @Override
    public void close() {
        idpDelegate.close();
    }

    @Override
    public IdentityProviderMapperModel createMapper(IdentityProviderMapperModel model) {
        return idpDelegate.createMapper(model);
    }

    @Override
    public void updateMapper(IdentityProviderMapperModel model) {
        registerIDPMapperInvalidation(model);
        idpDelegate.updateMapper(model);
    }

    @Override
    public boolean removeMapper(IdentityProviderMapperModel model) {
        registerIDPMapperInvalidation(model);
        return idpDelegate.removeMapper(model);
    }

    @Override
    public void removeAllMappers() {
        // no need to invalidate each entry in cache, removeAllMappers() is (currently) called only in case the realm is being deleted
        idpDelegate.removeAllMappers();
    }

    @Override
    public IdentityProviderMapperModel getMapperById(String id) {
        CachedIdentityProviderMapper cached = realmCache.getCache().get(id, CachedIdentityProviderMapper.class);
        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(id);
            IdentityProviderMapperModel model = idpDelegate.getMapperById(id);
            if (model == null) return null;
            if (isInvalid(id)) return model;
            cached = new CachedIdentityProviderMapper(loaded, getRealm(), id, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        } else if (isInvalid(id)) {
            return idpDelegate.getMapperById(id);
        }
        return cached.getIdentityProviderMapper();
    }

    @Override
    public IdentityProviderMapperModel getMapperByName(String identityProviderAlias, String name) {
        String cacheKey = cacheKeyIdpMapperAliasName(getRealm(), identityProviderAlias, name);

        if (isInvalid(cacheKey)) {
            return idpDelegate.getMapperByName(identityProviderAlias, name);
        }

        CachedIdentityProviderMapper cached = realmCache.getCache().get(cacheKey, CachedIdentityProviderMapper.class);

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            IdentityProviderMapperModel model = idpDelegate.getMapperByName(identityProviderAlias, name);
            if (model == null) return null;
            cached = new CachedIdentityProviderMapper(loaded, getRealm(), cacheKey, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return cached.getIdentityProviderMapper();
    }

    @Override
    public Stream<IdentityProviderMapperModel> getMappersStream(Map<String, String> options, Integer first, Integer max) {
        return idpDelegate.getMappersStream(options, first, max);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getMappersByAliasStream(String identityProviderAlias) {
        return idpDelegate.getMappersByAliasStream(identityProviderAlias);
    }

    private void registerIDPInvalidation(IdentityProviderModel idp) {
        realmCache.registerInvalidation(idp.getInternalId());
        realmCache.registerInvalidation(cacheKeyIdpAlias(getRealm(), idp.getAlias()));
    }

    private void registerCountInvalidation() {
        realmCache.registerInvalidation(cacheKeyIdpCount(getRealm()));
    }

    private void registerIDPMapperInvalidation(IdentityProviderMapperModel mapper) {
        if (mapper.getId() == null) {
            throw new ModelException("Identity Provider Mapper does not exist");
        }
        realmCache.registerInvalidation(mapper.getId());
        realmCache.registerInvalidation(cacheKeyIdpMapperAliasName(getRealm(), mapper.getIdentityProviderAlias(), mapper.getName()));
    }

    private RealmModel getRealm() {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
        return realm;
    }

    private boolean isInvalid(String cacheKey) {
        return realmCache.getInvalidations().contains(cacheKey);
    }

    private IdentityProviderModel createOrganizationAwareIdentityProviderModel(IdentityProviderModel idp) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) return idp;
        return new IdentityProviderModel(idp) {
            @Override
            public boolean isEnabled() {
                // if IdP is bound to an org
                if (getOrganizationId() != null) {
                    OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
                    OrganizationModel org = provider == null ? null : provider.getById(getOrganizationId());
                    return org != null && provider.isEnabled() && org.isEnabled() && super.isEnabled();
                }
                return super.isEnabled();
            }
        };
    }
}
