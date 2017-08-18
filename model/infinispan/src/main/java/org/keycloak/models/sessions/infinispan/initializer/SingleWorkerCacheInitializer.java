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

package org.keycloak.models.sessions.infinispan.initializer;

import java.io.Serializable;

import org.infinispan.Cache;
import org.keycloak.models.KeycloakSession;

/**
 * This impl is able to run the non-paginatable loader task and hence will be executed just on single node.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleWorkerCacheInitializer extends BaseCacheInitializer {

    private final KeycloakSession session;

    public SingleWorkerCacheInitializer(KeycloakSession session, Cache<String, Serializable> workCache, SessionLoader sessionLoader, String stateKeySuffix) {
        super(session.getKeycloakSessionFactory(), workCache, sessionLoader, stateKeySuffix, Integer.MAX_VALUE);
        this.session = session;
    }

    @Override
    protected void startLoading() {
        InitializerState state = getOrCreateInitializerState();
        while (!state.isFinished()) {
            sessionLoader.loadSessions(session, -1, -1);
            state.markSegmentFinished(0);
            saveStateToCache(state);
        }

        // Loader callback after the task is finished
        this.sessionLoader.afterAllSessionsLoaded(this);
    }
}
