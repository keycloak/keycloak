/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.grants.device.endpoints;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.Config;
import org.keycloak.cache.LocalCache;
import org.keycloak.cache.LocalCacheConfiguration;
import org.keycloak.cache.LocalCacheProvider;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceEndpointFactory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(DeviceEndpointFactory.class);

    public static final String PROVIDER_ID = "device";

    static final String CONFIG_USER_CODE_FAILURE_CACHE_SIZE = "userCodeFailureCacheSize";
    static final int DEFAULT_USER_CODE_FAILURE_CACHE_SIZE = 100_000;

    private int userCodeFailureCacheSize;
    private LocalCache<String, UserCodeFailureCounter> userCodeFailureCache;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        EventBuilder event = new EventBuilder(realm, session, context.getConnection());
        return new DeviceEndpoint(session, event);
    }

    @Override
    public void init(Config.Scope config) {
        userCodeFailureCacheSize = resolveUserCodeFailureCacheSize(config);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_USER_CODE_FAILURE_CACHE_SIZE)
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .helpText("Maximum number of realm and source address pairs tracked while rate limiting failed device user code verifications."
                        + " Entries are evicted once this size is reached, so a value smaller than the number of distinct source addresses"
                        + " seen within the device code lifespan weakens the limit. Non-positive values are ignored.")
                .defaultValue(DEFAULT_USER_CODE_FAILURE_CACHE_SIZE)
                .add()
                .build();
    }

    /**
     * Resolves {@link #CONFIG_USER_CODE_FAILURE_CACHE_SIZE}, rejecting non-positive values.
     * <p>
     * {@link LocalCacheConfiguration} treats any {@code maxSize <= 0} as "unbounded". This cache is keyed by a
     * caller-controlled source address on an unauthenticated endpoint, so silently turning it unbounded on a
     * misconfigured value would hand an attacker rotating source addresses a memory-exhaustion primitive; fall
     * back to the default instead.
     * <p>
     * Package-private so it can be unit tested without going through the full factory lifecycle.
     */
    int resolveUserCodeFailureCacheSize(Config.Scope config) {
        int configured = config.getInt(CONFIG_USER_CODE_FAILURE_CACHE_SIZE, DEFAULT_USER_CODE_FAILURE_CACHE_SIZE);

        if (configured <= 0) {
            logger.warnf("Ignoring non-positive value '%d' for '%s', which would make the cache unbounded. Using the default of %d instead.",
                    configured, CONFIG_USER_CODE_FAILURE_CACHE_SIZE, DEFAULT_USER_CODE_FAILURE_CACHE_SIZE);
            return DEFAULT_USER_CODE_FAILURE_CACHE_SIZE;
        }

        return configured;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            userCodeFailureCache = session.getProvider(LocalCacheProvider.class)
                    .create(LocalCacheConfiguration.<String, UserCodeFailureCounter>builder()
                            .name("device-user-code-failures")
                            .maxSize(userCodeFailureCacheSize)
                            // The window is the realm device code lifespan, so it varies per entry and is carried
                            // on the counter. Note the Duration overload of expirationAfterCreate currently
                            // delegates to after-access; only the BiFunction overload selects CREATE mode, which
                            // is what makes this a fixed window rather than one extended by every lookup.
                            .expirationAfterCreate((key, counter) -> counter.window())
                            .build());
        }
    }

    /**
     * Per-node counters of failed user code verifications, keyed by realm and source address.
     * <p>
     * Deliberately node-local rather than cluster-wide: the budget is therefore multiplied by the node count, but
     * that is a small number known to the operator, whereas keeping the counter in the shared single-use object
     * store would mean an unauthenticated caller could drive unbounded writes into storage shared with action
     * tokens, PAR and token revocation.
     */
    LocalCache<String, UserCodeFailureCounter> getUserCodeFailureCache() {
        return userCodeFailureCache;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(LocalCacheProvider.class); // for the user code verification failure counters
    }

    @Override
    public void close() {
        if (userCodeFailureCache != null) {
            userCodeFailureCache.close();
            userCodeFailureCache = null;
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * A failure count for one realm/source pair, plus the window it was created with so the cache can expire it.
     * <p>
     * The count is an {@link AtomicInteger} because the budget has to hold under concurrency, which is the whole
     * point of the limit: anything that lets an attacker with request concurrency C spend more than one attempt at
     * a time multiplies the effective budget by roughly C. That is also why the endpoint reserves its attempt with
     * {@link #incrementAndGet()} before it looks the code up, instead of reading the count first and incrementing
     * it afterwards, and gives the reservation back with {@link #decrement()} for a code that turns out to be
     * recognized.
     */
    static final class UserCodeFailureCounter {

        private final AtomicInteger failures = new AtomicInteger();
        private final Duration window;

        UserCodeFailureCounter(Duration window) {
            this.window = window;
        }

        Duration window() {
            return window;
        }

        int incrementAndGet() {
            return failures.incrementAndGet();
        }

        void decrement() {
            failures.decrementAndGet();
        }
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.DEVICE_FLOW);
    }
}