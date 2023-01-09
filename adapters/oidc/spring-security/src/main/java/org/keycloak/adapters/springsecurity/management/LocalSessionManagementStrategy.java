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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by scott on 4/24/15.
 */
public class LocalSessionManagementStrategy implements SessionManagementStrategy {

    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<String, HttpSession>();

    @Override
    public void clear() {
        sessions.clear();
    }

    @Override
    public Collection<HttpSession> getAll() {
        return sessions.values();
    }

    @Override
    public void store(HttpSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public HttpSession remove(String id) {
        return sessions.remove(id);
    }
}
