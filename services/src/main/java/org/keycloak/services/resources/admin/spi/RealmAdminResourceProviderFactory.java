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

package org.keycloak.services.resources.admin.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * This provider factory enables you to implement custom admin REST API extensions.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 */
public interface RealmAdminResourceProviderFactory extends ProviderFactory<RealmAdminResourceProvider> {

    /**
     * Create a RealmAdminResourceProvider with the keycloak session and admin auth.
     * Method overloaded from the default create(session) to provide access to
     * the realm, token, user and client of the current request.
     *
     * @param keycloakSession the keycloak session
     * @param adminAuth admin auth object containing the realm, token, user and client
     * @return a RealmAdminResourceProvider instance
     */
    RealmAdminResourceProvider create(KeycloakSession keycloakSession, AdminAuth adminAuth);

}
