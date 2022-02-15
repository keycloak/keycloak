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
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates parsing logic related to state passed to identity provider in "state" (or RelayState) parameter
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdentityBrokerState {

    private static final Pattern DOT = Pattern.compile("\\.");

    private static final int RELAY_STATE_MAX_LENGTH = 80;
    private static final Map<String, String> clientIdDigests = new HashMap<>();

    public static IdentityBrokerState decoded(String state, String clientId, String tabId) {
        String clientIdDigest = clientId;
        String encodedState = buildEncodedState(state, clientIdDigest, tabId);

        if(encodedState.length() > RELAY_STATE_MAX_LENGTH) {
            // Try to compress the clientId
            clientIdDigest = getClientIdDigest(clientIdDigest);
            encodedState = buildEncodedState(state, clientIdDigest, tabId);
        }

        return new IdentityBrokerState(state, clientIdDigest, tabId, encodedState);
    }


    public static IdentityBrokerState encoded(String encodedState) {
        String[] decoded = DOT.split(encodedState, 3);

        String state =(decoded.length > 0) ? decoded[0] : null;
        String tabId = (decoded.length > 1) ? decoded[1] : null;

        // The clientId may have been digested previously. In that case, replace it with the original clientId.
        String clientIdFromEncodedState = (decoded.length > 2) ? decoded[2] : null;
        String clientId = tryLookupClientIdFromDigest(clientIdFromEncodedState);

        return new IdentityBrokerState(state, clientId, tabId, encodedState);
    }


    private static String buildEncodedState(String state, String clientId, String tabId) {
        return state + "." + tabId + "." + clientId;
    }

    private static String getClientIdDigest(String clientId) {
        // Note: The map may be accessed concurrently, but a key always gets the same value (since String.hashCode() is deterministic),
        // so we don't need to worry about race conditions.
        clientIdDigests.put(clientId, Integer.toString(clientId.hashCode()));
        return clientIdDigests.get(clientId);
    }

    private static String tryLookupClientIdFromDigest(String clientIdDigest) {
        String foundClientId = null;

        for(String clientId : clientIdDigests.keySet()) {
            String digest = clientIdDigests.get(clientId);
            if(digest == null) {
                throw new IllegalStateException(String.format("Digest for clientId %s was unexpectedly null!", clientId));
            }

            if(digest.equals(clientIdDigest)) {
                if(foundClientId != null) {
                    throw new IllegalStateException(String.format("The clientId digest %s has multiple client id's mapped to it, impossible to reverse!", clientIdDigest));
                }
                foundClientId = clientId;
            }
        }

        return foundClientId != null ? foundClientId : clientIdDigest;
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
