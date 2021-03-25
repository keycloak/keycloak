/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.commons.api.BasicCache;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.models.OAuth2DeviceTokenStoreProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;

import java.util.function.Supplier;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class InfinispanOAuth2DeviceTokenStoreProviderFactory implements OAuth2DeviceTokenStoreProviderFactory {

    // Reuse "actionTokens" infinispan cache for now
    private volatile Supplier<BasicCache<String, ActionTokenValueEntity>> codeCache;

    @Override
    public OAuth2DeviceTokenStoreProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanOAuth2DeviceTokenStoreProvider(session, codeCache);
    }

    private void lazyInit(KeycloakSession session) {
        if (codeCache == null) {
            synchronized (this) {
                codeCache = InfinispanSingleUseTokenStoreProviderFactory.getActionTokenCache(session);
            }
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "infinispan";
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }
}
