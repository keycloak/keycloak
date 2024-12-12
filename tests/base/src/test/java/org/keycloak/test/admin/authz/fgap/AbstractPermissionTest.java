/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.test.admin.authz.fgap;

import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.models.Constants;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;

public abstract class AbstractPermissionTest {

    @InjectRealm(config = RealmAdminPermissionsConfig.class)
    ManagedRealm realm;

    @InjectClient(ref = Constants.ADMIN_PERMISSIONS_CLIENT_ID, createClient = false)
    ManagedClient client;

    protected PermissionsResource getPermissionsResource() {
        return client.admin().authorization().permissions();
    }

    protected PoliciesResource getPolicies() {
        return client.admin().authorization().policies();
    }

    protected ScopePermissionsResource getScopePermissionsResource() {
        return getPermissionsResource().scope();
    }
}
