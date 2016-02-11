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

package org.keycloak.services.resources.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;

/**
 * This provider factory enables you to implement custom (non-admin) REST API extensions.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 */
public interface RealmResourceProviderFactory extends ProviderFactory<RealmResourceProvider> {

    /**
     * Create a RealmResourceProvider with the keycloak session and realm.
     * Method overloaded from the default create(session) to provide access to
     * the realm of the current request.
     *
     * @param keycloakSession the keycloak session
     * @param realm the realm
     * @return a RealmResourceProvider instance
     */
    RealmResourceProvider create(KeycloakSession keycloakSession, RealmModel realm);

}
