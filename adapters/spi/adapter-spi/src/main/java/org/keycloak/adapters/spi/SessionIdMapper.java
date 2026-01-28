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

package org.keycloak.adapters.spi;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SessionIdMapper {
    /**
     * Returns {@code true} if the mapper contains mapping for the given HTTP session ID.
     * @param id
     * @return
     */
    boolean hasSession(String id);

    /**
     * Clears all mappings from this mapper.
     */
    void clear();

    /**
     * Returns set of HTTP session IDs for the given principal.
     * @param principal Principal
     * @return
     */
    Set<String> getUserSessions(String principal);

    /**
     * Returns HTTP session ID from the given user session ID.
     * @param sso User session ID
     * @return
     */
    String getSessionFromSSO(String sso);

    /**
     * Establishes mapping between user session ID, principal and HTTP session ID.
     * @param sso User session ID
     * @param principal Principal
     * @param session HTTP session ID
     */
    void map(String sso, String principal, String session);

    /**
     * Removes mappings for the given HTTP session ID.
     * @param session HTTP session ID.
     */
    void removeSession(String session);
}
