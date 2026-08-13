/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.session;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.session.PersistentAuthenticatedClientSessionAdapter;

class ClientSessionLoader implements Consumer<Map<String, AuthenticatedClientSessionModel>> {

    private boolean loaded = false;
    private final Supplier<Stream<PersistentAuthenticatedClientSessionAdapter>> supplier;

    ClientSessionLoader(Supplier<Stream<PersistentAuthenticatedClientSessionAdapter>> supplier) {
        assert supplier != null;
        this.supplier = supplier;
    }


    @Override
    public void accept(Map<String, AuthenticatedClientSessionModel> clientSessions) {
        if (loaded) {
            return;
        }
        supplier.get().forEach(m -> clientSessions.put(m.getClient().getId(), m));
        loaded = true;
    }
}
