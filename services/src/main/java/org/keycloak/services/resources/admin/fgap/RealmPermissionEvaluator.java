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
package org.keycloak.services.resources.admin.fgap;

import org.keycloak.authorization.model.ResourceServer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmPermissionEvaluator {
    boolean canListRealms();

    void requireViewRealmNameList();

    boolean canManageRealm();

    void requireManageRealm();

    boolean canViewRealm();

    void requireViewRealm();

    boolean canManageIdentityProviders();

    boolean canViewIdentityProviders();

    void requireViewIdentityProviders();

    void requireManageIdentityProviders();

    boolean canManageAuthorization(ResourceServer resourceServer);

    boolean canViewAuthorization(ResourceServer resourceServer);

    void requireManageAuthorization(ResourceServer resourceServer);

    void requireViewAuthorization(ResourceServer resourceServer);

    boolean canManageEvents();

    void requireManageEvents();

    boolean canViewEvents();

    void requireViewEvents();

    void requireViewRequiredActions();

    void requireViewAuthenticationFlows();

    void requireViewClientAuthenticatorProviders();
}
