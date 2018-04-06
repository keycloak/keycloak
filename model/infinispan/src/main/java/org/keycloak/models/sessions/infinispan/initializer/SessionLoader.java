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

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SessionLoader {

    /**
     * Will be triggered just once on cluster coordinator node to perform some generic initialization tasks (Eg. update DB before starting load).
     *
     * NOTE: This shouldn't be used for the initialization of loader instance itself!
     *
     * @param session
     */
    void init(KeycloakSession session);


    /**
     * Will be triggered just once on cluster coordinator node to count the number of sessions
     *
     * @param session
     * @return
     */
    int getSessionsCount(KeycloakSession session);


    /**
     * Will be called on all cluster nodes to load the specified page.
     *
     * @param session
     * @param first
     * @param max
     * @return
     */
    boolean loadSessions(KeycloakSession session, int first, int max);


    /**
     * This will be called on nodes to check if loading is finished. It allows loader to notify that loading is finished for some reason.
     *
     * @param initializer
     * @return
     */
    boolean isFinished(BaseCacheInitializer initializer);


    /**
     * Callback triggered on cluster coordinator once it recognize that all sessions were successfully loaded
     *
     * @param initializer
     */
    void afterAllSessionsLoaded(BaseCacheInitializer initializer);
}
