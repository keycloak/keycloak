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
 * Not Thread-safe
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdentityBrokerState {

    private String decodedState;
    private String clientId;
    private String encodedState;

    private IdentityBrokerState() {
    }

    public static IdentityBrokerState decoded(String decodedState, String clientId) {
        IdentityBrokerState state = new IdentityBrokerState();
        state.decodedState = decodedState;
        state.clientId = clientId;
        return state;
    }

    public static IdentityBrokerState encoded(String encodedState) {
        IdentityBrokerState state = new IdentityBrokerState();
        state.encodedState = encodedState;
        return state;
    }


    public String getDecodedState() {
        if (decodedState == null) {
            decode();
        }
        return decodedState;
    }

    public String getClientId() {
        if (decodedState == null) {
            decode();
        }
        return clientId;
    }

    public String getEncodedState() {
        if (encodedState == null) {
            encode();
        }
        return encodedState;
    }


    private void decode() {
        String[] decoded = DOT.split(encodedState, 0);
        decodedState = decoded[0];
        if (decoded.length > 0) {
            clientId = decoded[1];
        }
    }


    private void encode() {
        encodedState = decodedState + "." + clientId;
    }

    private static final Pattern DOT = Pattern.compile("\\.");


}
