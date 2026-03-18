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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageProviderFactory;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanPublicKeyStorageProviderFactory implements PublicKeyStorageProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanPublicKeyStorageProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    private volatile Cache<String, PublicKeysEntry> keysCache;

    private final Map<String, FutureTask<PublicKeysEntry>> tasksInProgress = new ConcurrentHashMap<>();

    private int minTimeBetweenRequests;
    private int maxCacheTime;

    @Override
    public PublicKeyStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanPublicKeyStorageProvider(session, keysCache, tasksInProgress, minTimeBetweenRequests, maxCacheTime);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name("minTimeBetweenRequests")
                    .type("int")
                    .helpText("Minimum interval in seconds between two requests to retrieve the new public keys. "
                            + "The server will always try to download new public keys when a single key is requested and not found. "
                            + "However it will avoid the download if the previous refresh was done less than 10 seconds ago (by default). "
                            + "This behavior is used to avoid DoS attacks against the external keys endpoint.")
                    .defaultValue(10)
                    .add()
                .property()
                    .name("maxCacheTime")
                    .type("int")
                    .helpText("Maximum interval in seconds that keys are cached when they are retrieved via all keys methods. "
                            + "When all keys for the entry are retrieved there is no way to detect if a key is missing "
                            + "(different to the case when the key is retrieved via ID for example). "
                            + "In that situation this option forces a refresh from time to time. "
                            + "This time can be overriden by the protocol (for example using cacheDuration or validUntil in the SAML descriptor). "
                            + "Default 24 hours.")
                    .defaultValue(24*60*60)
                    .add()
                .build();
    }

    private void lazyInit(KeycloakSession session) {
        if (keysCache == null) {
            synchronized (this) {
                if (keysCache == null) {
                    this.keysCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME);
                }
            }
        }
    }

    @Override
    public void init(Config.Scope config) {
        // minTimeBetweenRequests is used when getting a key via name or
        // predicate to avoid doing calls very sooon when a key is missing
        minTimeBetweenRequests = config.getInt("minTimeBetweenRequests", 10);

        // maxCacheTime is used to reload keys when retrieved via all getKeys
        // a refresh is ensured for that method from time to time
        maxCacheTime = config.getInt("maxCacheTime", 24*60*60); // 24 hours

        log.debugf("minTimeBetweenRequests is %d maxCacheTime is %d", minTimeBetweenRequests, maxCacheTime);
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
