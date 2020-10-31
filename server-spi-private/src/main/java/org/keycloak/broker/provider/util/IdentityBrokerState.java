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

import com.google.common.primitives.Bytes;
import org.keycloak.common.util.Base64Url;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Encapsulates parsing logic related to state passed to identity provider in "state" (or RelayState) parameter
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdentityBrokerState {

    public static IdentityBrokerState decoded(String state, String clientId, String tabId) {
        byte[] stateBytes = Base64Url.decode(state);
        byte[] tabIdBytes = Base64Url.decode(tabId);
        byte[] clientIdBytes = clientId.getBytes();
        byte[] indexBytes = new byte[2];
        indexBytes[0] = (byte) stateBytes.length;
        indexBytes[1] = (byte) tabIdBytes.length;

        String encodedState = Base64Url.encode(Bytes.concat(indexBytes, stateBytes, tabIdBytes, clientIdBytes));

        return new IdentityBrokerState(state, clientId, tabId, encodedState);
    }


    public static IdentityBrokerState encoded(String encodedState) {
        byte[] decoded = Base64Url.decode(encodedState);

        int stateStart = 2;
        int tabIdStart = 2 + decoded[0];
        int clientIdStart = 2 + decoded[0] + decoded[1];

        byte[] stateBytes = Arrays.copyOfRange(decoded, stateStart, tabIdStart);
        byte[] tabIdBytes = Arrays.copyOfRange(decoded, tabIdStart, clientIdStart);
        byte[] clientIdBytes = Arrays.copyOfRange(decoded, clientIdStart, decoded.length);

        return new IdentityBrokerState(Base64Url.encode(stateBytes), new String(clientIdBytes), Base64Url.encode(tabIdBytes), encodedState);
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
