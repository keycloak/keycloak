/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keys.infinispan;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageProviderFactory;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanPublicKeyStorageProviderFactory implements PublicKeyStorageProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanPublicKeyStorageProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public static final String KEYS_CLEAR_CACHE_EVENTS = "KEYS_CLEAR_CACHE_EVENTS";

    public static final String PUBLIC_KEY_STORAGE_INVALIDATION_EVENT = "PUBLIC_KEY_STORAGE_INVALIDATION_EVENT";

    private volatile Cache<String, PublicKeysEntry> keysCache;

    private final Map<String, FutureTask<PublicKeysEntry>> tasksInProgress = new ConcurrentHashMap<>();

    private int minTimeBetweenRequests;

    @Override
    public PublicKeyStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanPublicKeyStorageProvider(session, keysCache, tasksInProgress, minTimeBetweenRequests);
    }

    private void lazyInit(KeycloakSession session) {
        if (keysCache == null) {
            synchronized (this) {
                if (keysCache == null) {
                    this.keysCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME);

                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);
                    cluster.registerListener(PUBLIC_KEY_STORAGE_INVALIDATION_EVENT, (ClusterEvent event) -> {

                        PublicKeyStorageInvalidationEvent invalidationEvent = (PublicKeyStorageInvalidationEvent) event;
                        keysCache.remove(invalidationEvent.getCacheKey());

                    });

                    cluster.registerListener(KEYS_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> {

                        keysCache.clear();

                    });
                }
            }
        }
    }

    @Override
    public void init(Config.Scope config) {
        minTimeBetweenRequests = config.getInt("minTimeBetweenRequests", 10);
        log.debugf("minTimeBetweenRequests is %d", minTimeBetweenRequests);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (keysCache == null) {
                    return;
                }

                SessionAndKeyHolder cacheKey = getCacheKeyToInvalidate(event);
                if (cacheKey != null) {
                    log.debugf("Invalidating %s from keysCache", cacheKey);
                    InfinispanPublicKeyStorageProvider provider = (InfinispanPublicKeyStorageProvider) cacheKey.session.getProvider(PublicKeyStorageProvider.class, getId());
                    for (String ck : cacheKey.cacheKeys) provider.addInvalidation(ck);
                }
            }

        });
    }

    private SessionAndKeyHolder getCacheKeyToInvalidate(ProviderEvent event) {
        ArrayList<String> cacheKeys = new ArrayList<>();
        String cacheKey = null;
        if (event instanceof ClientModel.ClientUpdatedEvent) {
            ClientModel.ClientUpdatedEvent eventt = (ClientModel.ClientUpdatedEvent) event;
            cacheKey = PublicKeyStorageUtils.getClientModelCacheKey(eventt.getUpdatedClient().getRealm().getId(), eventt.getUpdatedClient().getId(), JWK.Use.SIG);
            cacheKeys.add(cacheKey);
            cacheKey = PublicKeyStorageUtils.getClientModelCacheKey(eventt.getUpdatedClient().getRealm().getId(), eventt.getUpdatedClient().getId(), JWK.Use.ENCRYPTION);
            cacheKeys.add(cacheKey);
            return new SessionAndKeyHolder(eventt.getKeycloakSession(), cacheKeys);
        } else if (event instanceof ClientModel.ClientRemovedEvent) {
            ClientModel.ClientRemovedEvent eventt = (ClientModel.ClientRemovedEvent) event;
            cacheKey = PublicKeyStorageUtils.getClientModelCacheKey(eventt.getClient().getRealm().getId(), eventt.getClient().getId(), JWK.Use.SIG);
            cacheKeys.add(cacheKey);
            cacheKey = PublicKeyStorageUtils.getClientModelCacheKey(eventt.getClient().getRealm().getId(), eventt.getClient().getId(), JWK.Use.ENCRYPTION);
            cacheKeys.add(cacheKey);
            return new SessionAndKeyHolder(eventt.getKeycloakSession(), cacheKeys);
        } else if (event instanceof RealmModel.IdentityProviderUpdatedEvent) {
            RealmModel.IdentityProviderUpdatedEvent eventt = (RealmModel.IdentityProviderUpdatedEvent) event;
            cacheKey = PublicKeyStorageUtils.getIdpModelCacheKey(eventt.getRealm().getId(), eventt.getUpdatedIdentityProvider().getInternalId());
            cacheKeys.add(cacheKey);
            return new SessionAndKeyHolder(eventt.getKeycloakSession(), cacheKeys);
        } else if (event instanceof RealmModel.IdentityProviderRemovedEvent) {
            RealmModel.IdentityProviderRemovedEvent eventt = (RealmModel.IdentityProviderRemovedEvent) event;
            cacheKey = PublicKeyStorageUtils.getIdpModelCacheKey(eventt.getRealm().getId(), eventt.getRemovedIdentityProvider().getInternalId());
            cacheKeys.add(cacheKey);
            return new SessionAndKeyHolder(eventt.getKeycloakSession(), cacheKeys);
        } else {
            return null;
        }
    }

    private static class SessionAndKeyHolder {
        private final KeycloakSession session;
        private final ArrayList<String> cacheKeys;

        public SessionAndKeyHolder(KeycloakSession session, ArrayList<String> cacheKeys) {
            this.session = session;
            this.cacheKeys = cacheKeys;
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
