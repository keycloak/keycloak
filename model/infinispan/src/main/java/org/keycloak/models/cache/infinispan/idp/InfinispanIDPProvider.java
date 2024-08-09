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

import java.util.Map;
import java.util.stream.Stream;
import org.keycloak.models.IDPProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.RealmCacheSession;

public class InfinispanIDPProvider implements IDPProvider {

    private static final String IDP_COUNT_KEY_SUFFIX = ".idp.count";
    private static final String IDP_ALIAS_KEY_SUFFIX = ".idp.alias";

    private final KeycloakSession session;
    private final IDPProvider idpDelegate;
    private final RealmCacheSession realmCache;
    
    public InfinispanIDPProvider(KeycloakSession session) {
        this.session = session;
        this.idpDelegate = session.getProvider(IDPProvider.class, "jpa");
        this.realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
    }

    static String cacheKeyIdpCount(RealmModel realm) {
        return realm.getId() + IDP_COUNT_KEY_SUFFIX;
    }

    static String cacheKeyIdpAlias(RealmModel realm, String alias) {
        return realm.getId() + "." + alias + IDP_ALIAS_KEY_SUFFIX;
    }

    @Override
    public IdentityProviderModel create(IdentityProviderModel model) {
        registerCountInvalidation();
        return idpDelegate.create(model);
    }

    @Override
    public void update(IdentityProviderModel model) {
        registerIDPInvalidation(model);
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
        CachedIdentityProvider cached = realmCache.getCache().get(internalId, CachedIdentityProvider.class);
        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(internalId);
            IdentityProviderModel model = idpDelegate.getById(internalId);
            if (model == null) return null;
            if (isInvalid(internalId)) return model;
            cached = new CachedIdentityProvider(loaded, getRealm(), internalId, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        } else if (isInvalid(internalId)) {
            return idpDelegate.getById(internalId);
        }
        return cached.getIdentityProvider();
    }

    @Override
    public IdentityProviderModel getByAlias(String alias) {
        String cacheKey = cacheKeyIdpAlias(getRealm(), alias);

        if (isInvalid(cacheKey)) {
            return idpDelegate.getByAlias(alias);
        }

        CachedIdentityProvider cached = realmCache.getCache().get(cacheKey, CachedIdentityProvider.class);

        String realmId = getRealm().getId();
        if (cached != null && !cached.getRealm().equals(realmId)) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
            IdentityProviderModel model = idpDelegate.getByAlias(alias);
            if (model == null) {
                return null;
            }
            cached = new CachedIdentityProvider(loaded, getRealm(), cacheKey, model);
            realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());
        }

        return cached.getIdentityProvider();
    }

    @Override
    public Stream<IdentityProviderModel> getAllStream(String search, Integer first, Integer max) {
        return idpDelegate.getAllStream(search, first, max);
    }

    @Override
    public Stream<IdentityProviderModel> getAllStream(Map<String, String> attrs, Integer first, Integer max) {
        return idpDelegate.getAllStream(attrs, first, max);
    }

    @Override
    public long count() {
        String cacheKey = cacheKeyIdpCount(getRealm());
        CachedIdpCount cached = realmCache.getCache().get(cacheKey, CachedIdpCount.class);

        // cached and not invalidated
        if (cached != null && !isInvalid(cacheKey)) {
            return cached.getCount();
        }

        Long loaded = realmCache.getCache().getCurrentRevision(cacheKey);
        long count = idpDelegate.count();
        cached = new CachedIdpCount(loaded, getRealm(), count);
        realmCache.getCache().addRevisioned(cached, realmCache.getStartupRevision());

        return count;
    }

    @Override
    public void close() {
        idpDelegate.close();
    }

    private void registerIDPInvalidation(IdentityProviderModel idp) {
        realmCache.registerInvalidation(idp.getInternalId());
        realmCache.registerInvalidation(cacheKeyIdpAlias(getRealm(), idp.getAlias()));
    }

    private void registerCountInvalidation() {
        realmCache.registerInvalidation(cacheKeyIdpCount(getRealm()));
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
}
