/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import java.io.Closeable;
import java.io.IOException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 *  Creates a temporary realm and makes sure it is removed.
 */
public class IdentityProviderCreator implements Closeable {

    private final IdentityProvidersResource resource;
    private final String alias;

    public IdentityProviderCreator(RealmResource realmResource, IdentityProviderRepresentation rep) {
        resource = realmResource.identityProviders();
        alias = rep.getAlias();
        Response response = null;
        try {
            response = resource.create(rep);
        } finally {
            if (response != null)
                response.close();
        }
    }

    public IdentityProvidersResource resource() {
        return this.resource;
    }

    public IdentityProviderResource identityProvider() {
        return this.resource().get(alias);
    }

    @Override
    public void close() throws IOException {
        try {
            resource.get(alias).remove();
        } catch (NotFoundException e) {
            // ignore
        }
    }
}
