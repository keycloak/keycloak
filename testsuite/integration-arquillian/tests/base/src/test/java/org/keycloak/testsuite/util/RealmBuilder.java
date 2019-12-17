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
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    public RealmBuilder roles(RolesBuilder roles) {
        return roles(roles.build());
    }

    public RealmBuilder roles(RolesRepresentation roles) {
        rep.setRoles(roles);
        return this;
    }

    public RealmBuilder events() {
        rep.setEventsEnabled(true);
        rep.setEnabledEventTypes(Collections.<String>emptyList()); // enables all types
        return this;
    }

    public RealmBuilder attribute(String key, String value) {
        if (rep.getAttributes() == null) {
            rep.setAttributes(new HashMap<>());
        }
        rep.getAttributes().put(key, value);
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

        if (!rep.getEventsListeners().contains(EventsListenerProviderFactory.PROVIDER_ID)) {
            rep.getEventsListeners().add(EventsListenerProviderFactory.PROVIDER_ID);
        }

        return this;
    }

    public RealmBuilder removeTestEventListener() {
        if (rep.getEventsListeners() != null && rep.getEventsListeners().contains(EventsListenerProviderFactory.PROVIDER_ID)) {
            rep.getEventsListeners().remove(EventsListenerProviderFactory.PROVIDER_ID);
        }

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

    public RealmBuilder notBefore(int i) {
        rep.setNotBefore(i);
        return this;
    }

    public RealmBuilder otpLookAheadWindow(int i) {
        rep.setOtpPolicyLookAheadWindow(i);
        return this;
    }

    public RealmBuilder bruteForceProtected(boolean bruteForceProtected) {
        rep.setBruteForceProtected(bruteForceProtected);
        return this;
    }

    public RealmBuilder failureFactor(int failureFactor) {
        rep.setFailureFactor(failureFactor);
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

    public RealmBuilder passwordPolicy(String passwordPolicy) {
        rep.setPasswordPolicy(passwordPolicy);
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

    public RealmBuilder accessTokenLifespan(int accessTokenLifespan) {
        rep.setAccessTokenLifespan(accessTokenLifespan);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespan(int ssoSessionMaxLifespan) {
        rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeoutRememberMe(int ssoSessionIdleTimeoutRememberMe){
        rep.setSsoSessionIdleTimeoutRememberMe(ssoSessionIdleTimeoutRememberMe);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespanRememberMe(int ssoSessionMaxLifespanRememberMe){
        rep.setSsoSessionMaxLifespanRememberMe(ssoSessionMaxLifespanRememberMe);
        return this;
    }

    public RealmBuilder accessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        rep.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        return this;
    }

    public RealmBuilder accessCodeLifespan(int accessCodeLifespan) {
        rep.setAccessCodeLifespan(accessCodeLifespan);
        return this;
    }

    public RealmBuilder sslRequired(String sslRequired) {
        rep.setSslRequired(sslRequired);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeout(int sessionIdleTimeout) {
        rep.setSsoSessionIdleTimeout(sessionIdleTimeout);
        return this;
    }

    public RealmBuilder group(GroupRepresentation group) {
        if (rep.getGroups() == null) {
            rep.setGroups(new ArrayList<>());
        }
        rep.getGroups().add(group);
        return this;
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    public RealmBuilder offlineSessionIdleTimeout(int offlineSessionIdleTimeout) {
        rep.setOfflineSessionIdleTimeout(offlineSessionIdleTimeout);
        return this;
    }

    public RealmBuilder offlineSessionMaxLifespan(int offlineSessionMaxLifespan) {
        rep.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan);
        return this;
    }

    public RealmBuilder offlineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        rep.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
        return this;
    }
}
