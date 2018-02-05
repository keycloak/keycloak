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

import org.keycloak.admin.client.Keycloak;
import java.io.Closeable;
import javax.ws.rs.NotFoundException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import java.io.IOException;

/**
 *  Creates a temporary realm and makes sure it is removed.
 */
public class RealmCreator implements Closeable {

    private final RealmResource realmResource;

    public RealmCreator(Keycloak adminClient, RealmRepresentation rep) {
        adminClient.realms().create(rep);
        this.realmResource = adminClient.realm(rep.getRealm());
    }

    public RealmResource realm() {
        return this.realmResource;
    }

    @Override
    public void close() throws IOException {
        try {
            realmResource.remove();
        } catch (NotFoundException e) {
            // ignore
        }
    }
}
