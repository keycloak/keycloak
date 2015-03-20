/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.broker.provider;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Pedro Igor
 */
public abstract class AbstractIdentityProvider<C extends IdentityProviderModel> implements IdentityProvider<C> {

    private final C config;

    public AbstractIdentityProvider(C config) {
        this.config = config;
    }

    public C getConfig() {
        return this.config;
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        return Response.noContent().build();
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback) {
        return null;
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        return null;
    }
}
