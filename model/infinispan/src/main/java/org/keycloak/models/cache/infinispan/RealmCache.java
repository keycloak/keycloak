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

package org.keycloak.models.cache.infinispan;

import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.entities.CachedRole;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmCache {
    void clear();

    CachedRealm getRealm(String id);

    void invalidateRealm(CachedRealm realm);

    void addRealm(CachedRealm realm);

    CachedRealm getRealmByName(String name);

    void invalidateRealmById(String id);

    CachedClient getClient(String id);

    void invalidateClient(CachedClient app);

    void evictClientById(String id);

    void addClient(CachedClient app);

    void invalidateClientById(String id);

    CachedRole getRole(String id);

    void invalidateRole(CachedRole role);

    void evictRoleById(String id);

    void addRole(CachedRole role);

    void invalidateRoleById(String id);

    CachedGroup getGroup(String id);

    void invalidateGroup(CachedGroup role);

    void addGroup(CachedGroup role);

    void invalidateGroupById(String id);

    CachedClientTemplate getClientTemplate(String id);

    void invalidateClientTemplate(CachedClientTemplate app);

    void evictClientTemplateById(String id);

    void addClientTemplate(CachedClientTemplate app);

    void invalidateClientTemplateById(String id);

}
