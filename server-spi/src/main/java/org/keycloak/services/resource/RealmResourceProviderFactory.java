/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resource;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

/**
 * <p>A factory that creates {@link RealmResourceProvider} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface RealmResourceProviderFactory extends ProviderFactory<RealmResourceProvider>, ServerInfoAwareProviderFactory {

    /**
     * Creates a {@link RealmResourceProvider}.
     *
     * @param realm the {@link RealmModel} associated with the current request
     * @param keycloakSession the {@link KeycloakSession} associated with the current request
     * @return a {@link RealmResourceProvider} instance
     */
    RealmResourceProvider create(RealmModel realm, KeycloakSession keycloakSession);
}