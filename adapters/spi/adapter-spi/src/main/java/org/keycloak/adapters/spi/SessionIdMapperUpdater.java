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
package org.keycloak.adapters.spi;

/**
 * Classes implementing this interface represent a mechanism for updating {@link SessionIdMapper} entries.
 * @author hmlnarik
 */
public interface SessionIdMapperUpdater {
    /**
     * {@link SessionIdMapper} entries are updated directly.
     */
    public static final SessionIdMapperUpdater DIRECT = new SessionIdMapperUpdater() {
        @Override public void clear(SessionIdMapper idMapper) {
            idMapper.clear();
        }

        @Override public void map(SessionIdMapper idMapper, String sso, String principal, String httpSessionId) {
            idMapper.map(sso, principal, httpSessionId);
        }

        @Override public void removeSession(SessionIdMapper idMapper, String httpSessionId) {
            idMapper.removeSession(httpSessionId);
        }
    };

    /**
     * Only HTTP session is manipulated with, {@link SessionIdMapper} entries are not updated by this updater and
     * they have to be updated by some other means, e.g. by some listener of HTTP session changes.
     */
    public static final SessionIdMapperUpdater EXTERNAL = new SessionIdMapperUpdater() {
        @Override public void clear(SessionIdMapper idMapper) { }

        @Override public void map(SessionIdMapper idMapper, String sso, String principal, String httpSessionId) { }

        @Override public void removeSession(SessionIdMapper idMapper, String httpSessionId) { }
    };

    /**
     * Delegates to {@link SessionIdMapper#clear} method..
     */
    public abstract void clear(SessionIdMapper idMapper);

    /**
     * Delegates to {@link SessionIdMapper#map} method.
     * @param idMapper Mapper
     * @param sso User session ID
     * @param principal Principal
     * @param session HTTP session ID
     */
    public abstract void map(SessionIdMapper idMapper, String sso, String principal, String session);

    /**
     * Delegates to {@link SessionIdMapper#removeSession} method.
     * @param idMapper Mapper
     * @param session HTTP session ID.
     */
    public abstract void removeSession(SessionIdMapper idMapper, String session);
}
