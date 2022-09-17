/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources.admin;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CachePublicKeyProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.POST;
import javax.ws.rs.core.Context;

public class ClearKeysCacheResource {

    protected AdminPermissionEvaluator auth;
    protected RealmModel realm;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClearKeysCacheResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent;
        this.session = session;
    }

    /**
     * Clear cache of external public keys (Public keys of clients or Identity providers)
     *
     */
    @POST
    public void clearKeysCache() {
        auth.realm().requireManageRealm();

        CachePublicKeyProvider cache = session.getProvider(CachePublicKeyProvider.class);
        if (cache != null) {
            cache.clearCache();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }
}
