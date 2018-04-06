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

package org.keycloak.broker.provider.util;

import java.util.regex.Pattern;

/**
 * Encapsulates parsing logic related to state passed to identity provider in "state" (or RelayState) parameter
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdentityBrokerState {

    private static final Pattern DOT = Pattern.compile("\\.");


    public static IdentityBrokerState decoded(String state, String clientId, String tabId) {
        String encodedState = state + "." + tabId + "." + clientId;

        return new IdentityBrokerState(state, clientId, tabId, encodedState);
    }


    public static IdentityBrokerState encoded(String encodedState) {
        String[] decoded = DOT.split(encodedState, 3);

        String state =(decoded.length > 0) ? decoded[0] : null;
        String tabId = (decoded.length > 1) ? decoded[1] : null;
        String clientId = (decoded.length > 2) ? decoded[2] : null;

        return new IdentityBrokerState(state, clientId, tabId, encodedState);
    }



    private final String decodedState;
    private final String clientId;
    private final String tabId;

    // Encoded form of whole state
    private final String encoded;

    private IdentityBrokerState(String decodedStateParam, String clientId, String tabId, String encoded) {
        this.decodedState = decodedStateParam;
        this.clientId = clientId;
        this.tabId = tabId;
        this.encoded = encoded;
    }


    public String getDecodedState() {
        return decodedState;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTabId() {
        return tabId;
    }

    public String getEncoded() {
        return encoded;
    }
}
