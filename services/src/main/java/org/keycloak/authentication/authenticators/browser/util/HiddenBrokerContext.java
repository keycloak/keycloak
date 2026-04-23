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

package org.keycloak.authentication.authenticators.browser.util;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

public class HiddenBrokerContext {

    private Set<String> hiddenBrokers = new LinkedHashSet<>();

    public Set<String> getHiddenBrokers() {
        return hiddenBrokers;
    }

    public void setHiddenBrokers(Collection<String> hiddenBrokers) {
        this.hiddenBrokers.clear();
        this.hiddenBrokers.addAll(hiddenBrokers);
    }

    public void addHiddenBroker(String brokerAliasId) {
        hiddenBrokers.add(brokerAliasId);
    }

    // Save this context as note to authSession
    public void saveToAuthenticationSession(AuthenticationSessionModel authSession) {
        try {
            String asString = JsonSerialization.writeValueAsString(this);
            authSession.setAuthNote(AbstractUsernameFormAuthenticator.HIDDEN_BROKERS, asString);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static HiddenBrokerContext readFromAuthenticationSession(AuthenticationSessionModel authSession) {
        String asString = authSession.getAuthNote(AbstractUsernameFormAuthenticator.HIDDEN_BROKERS);
        if (asString == null) {
            return null;
        } else {
            try {
                return JsonSerialization.readValue(asString, HiddenBrokerContext.class);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }
}
