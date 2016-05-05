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
import sun.security.krb5.Realm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT;

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

    public RealmBuilder testMail() {
        Map<String, String> config = new HashMap<>();
        config.put("from", MailServerConfiguration.FROM);
        config.put("host", MailServerConfiguration.HOST);
        config.put("port", MailServerConfiguration.PORT);
        rep.setSmtpServer(config);
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

    public RealmBuilder otpLookAheadWindow(int i) {
        rep.setOtpPolicyLookAheadWindow(i);
        return this;
    }

    public RealmBuilder otpDigits(int i) {
        rep.setOtpPolicyDigits(i);
        return this;
    }

    public RealmBuilder otpPeriod(int i) {
        rep.setOtpPolicyPeriod(i);
        return this;
    }

    public RealmBuilder otpType(String type) {
        rep.setOtpPolicyType(type);
        return this;
    }

    public RealmBuilder otpAlgorithm(String algorithm) {
        rep.setOtpPolicyAlgorithm(algorithm);
        return this;
    }

    public RealmBuilder otpInitialCounter(int i) {
        rep.setOtpPolicyInitialCounter(i);
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

    public RealmBuilder accessTokenLifespan(int accessTokenLifespan) {
        rep.setAccessTokenLifespan(accessTokenLifespan);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeout(int sessionIdleTimeout) {
        rep.setSsoSessionIdleTimeout(sessionIdleTimeout);
        return this;
    }
}
