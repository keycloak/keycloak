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
package org.keycloak.adapters.saml.jbossweb.infinispan;

import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class SsoCacheSessionIdMapperUpdater implements SessionIdMapperUpdater {

    private final SessionIdMapperUpdater delegate;
    /**
     * Cache where key is a HTTP session ID, and value is a pair (user session ID, principal name) of Strings.
     */
    private final Cache<String, String[]> httpSessionToSsoCache;

    public SsoCacheSessionIdMapperUpdater(Cache<String, String[]> httpSessionToSsoCache, SessionIdMapperUpdater previousIdMapperUpdater) {
        this.delegate = previousIdMapperUpdater;
        this.httpSessionToSsoCache = httpSessionToSsoCache;
    }

    // SessionIdMapperUpdater methods

    @Override
    public void clear(SessionIdMapper idMapper) {
        httpSessionToSsoCache.clear();
        this.delegate.clear(idMapper);
    }

    @Override
    public boolean refreshMapping(SessionIdMapper idMapper, String httpSessionId) {
        String[] ssoAndPrincipal = httpSessionToSsoCache.get(httpSessionId);
        if (ssoAndPrincipal != null) {
            this.delegate.map(idMapper, ssoAndPrincipal[0], ssoAndPrincipal[1], httpSessionId);
            return true;
        }
        return false;
    }

    @Override
    public void map(SessionIdMapper idMapper, String sso, String principal, String httpSessionId) {
        httpSessionToSsoCache.put(httpSessionId, new String[] {sso, principal});
        this.delegate.map(idMapper, sso, principal, httpSessionId);
    }

    @Override
    public void removeSession(SessionIdMapper idMapper, String httpSessionId) {
        httpSessionToSsoCache.remove(httpSessionId);
        this.delegate.removeSession(idMapper, httpSessionId);
    }
}
