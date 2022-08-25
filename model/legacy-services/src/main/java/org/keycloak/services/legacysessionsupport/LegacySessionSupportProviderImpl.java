/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.legacysessionsupport;

import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.LegacySessionSupportProvider;
import org.keycloak.models.cache.UserCache;

/**
 * @author Alexander Schwartz
 */
public class LegacySessionSupportProviderImpl implements LegacySessionSupportProvider {

    private final KeycloakSession session;

    public LegacySessionSupportProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {

    }

    @Override
    @Deprecated
    public UserCredentialManager userCredentialManager() {
        // UserCacheSession calls session.userCredentialManager().onCache(), therefore can't trigger a warning here at the moment.
        return new UserCredentialStoreManager(session);
    }

    @Override
    public UserCache userCache() {
        return session.getProvider(UserCache.class);
    }

}
