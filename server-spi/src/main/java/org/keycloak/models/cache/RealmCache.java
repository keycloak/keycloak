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

package org.keycloak.models.cache;

import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedClientTemplate;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmCache {
    void clear();

    CachedRealm getCachedRealm(String id);

    void invalidateCachedRealm(CachedRealm realm);

    void addCachedRealm(CachedRealm realm);

    CachedRealm getCachedRealmByName(String name);

    void invalidateCachedRealmById(String id);

    CachedClient getApplication(String id);

    void invalidateApplication(CachedClient app);

    void evictCachedApplicationById(String id);

    void addCachedClient(CachedClient app);

    void invalidateCachedApplicationById(String id);

    CachedRole getRole(String id);

    void invalidateRole(CachedRole role);

    void evictCachedRoleById(String id);

    void addCachedRole(CachedRole role);

    void invalidateCachedRoleById(String id);

    void invalidateRoleById(String id);

    CachedGroup getGroup(String id);

    void invalidateGroup(CachedGroup role);

    void addCachedGroup(CachedGroup role);

    void invalidateCachedGroupById(String id);

    void invalidateGroupById(String id);

    CachedClientTemplate getClientTemplate(String id);

    void invalidateClientTemplate(CachedClientTemplate app);

    void evictCachedClientTemplateById(String id);

    void addCachedClientTemplate(CachedClientTemplate app);

    void invalidateCachedClientTemplateById(String id);

}
