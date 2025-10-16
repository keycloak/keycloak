/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.ClientScopeRepresentation;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientScopeBuilder {

    private final ClientScopeRepresentation rep;

    public static ClientScopeBuilder create() {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        return new ClientScopeBuilder(rep);
    }

    public static ClientScopeBuilder edit(ClientScopeRepresentation rep) {
        return new ClientScopeBuilder(rep);
    }

    private ClientScopeBuilder(ClientScopeRepresentation rep) {
        this.rep = rep;
    }

    public ClientScopeRepresentation build() {
        return rep;
    }

    public ClientScopeBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientScopeBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public ClientScopeBuilder protocol(String protocol) {
        rep.setProtocol(protocol);
        return this;
    }
}
