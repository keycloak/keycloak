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

package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.LinkedList;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmBuilder {

    private final RealmRepresentation rep;

    public static RealmBuilder create() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(Boolean.TRUE);
        return new RealmBuilder(rep);
    }

    public static RealmBuilder edit(RealmRepresentation rep) {
        return new RealmBuilder(rep);
    }

    private RealmBuilder(RealmRepresentation rep) {
        this.rep = rep;
    }

    public RealmBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmBuilder publicKey(String publicKey) {
        rep.setPublicKey(publicKey);
        return this;
    }

    public RealmBuilder privateKey(String privateKey) {
        rep.setPrivateKey(privateKey);
        return this;
    }

    public RealmBuilder testEventListener() {
        if (rep.getEventsListeners() == null) {
            rep.setEventsListeners(new LinkedList<String>());
        }

        rep.getEventsListeners().add("event-queue");
        return this;
    }

    public RealmBuilder client(ClientBuilder client) {
        return client(client.build());
    }

    public RealmBuilder client(ClientRepresentation client) {
        if (rep.getClients() == null) {
            rep.setClients(new LinkedList<ClientRepresentation>());
        }
        rep.getClients().add(client);
        return this;
    }

    public RealmBuilder user(UserBuilder user) {
        return user(user.build());
    }

    public RealmBuilder user(UserRepresentation user) {
        if (rep.getUsers() == null) {
            rep.setUsers(new LinkedList<UserRepresentation>());
        }
        rep.getUsers().add(user);
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

}
