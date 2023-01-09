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

package org.keycloak.adapters.springsecurity.management;

import javax.servlet.http.HttpSession;
import java.util.Collection;

/**
 * Defines a session management strategy.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public interface SessionManagementStrategy {

    /**
     * Removes all sessions.
     */
    void clear();

    /**
     * Returns a collection containing all sessions.
     *
     * @return a <code>Collection</code> of all known <code>HttpSession</code>s, if any;
     * an empty <code>Collection</code> otherwise
     */
    Collection<HttpSession> getAll();

    /**
     * Stores the given session.
     *
     * @param session the <code>HttpSession</code> to store (required)
     */
    void store(HttpSession session);

    /**
     * The unique identifier for the session to remove.
     *
     * @param id the unique identifier for the session to remove (required)
     * @return the <code>HttpSession</code> if it exists; <code>null</code> otherwise
     */
    HttpSession remove(String id);
}
