/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import java.util.Map;
import java.util.function.Supplier;


/**
 * Extends the {@link InfinispanSingleUseObjectProvider} to add all newly revoked tokens to a persistent storage, so they
 * can be read again on Keycloak startup when necessary.
 *
 * @author Alexander Schwartz
 */
public class PersistentSessionsSingleUseObjectProvider extends InfinispanSingleUseObjectProvider {

    public static final Logger logger = Logger.getLogger(PersistentSessionsSingleUseObjectProvider.class);
    private final KeycloakSession session;

    public PersistentSessionsSingleUseObjectProvider(KeycloakSession session, Supplier<BasicCache<String, SingleUseObjectValueEntity>> singleUseObjectCache) {
        super(session, singleUseObjectCache);
        this.session = session;
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        super.put(key, lifespanSeconds, notes);
        if (key.endsWith(REVOKED_KEY)) {
            if (!notes.isEmpty()) {
                throw new ModelException("notes are not supported for revoked tokens");
            }
            session.getProvider(RevokedTokenPersisterProvider.class).revokeToken(key.substring(0, key.length() - REVOKED_KEY.length()), lifespanSeconds);
        }
    }

}
