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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import org.keycloak.common.util.Base64Url;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * Encapsulates parsing logic related to state passed to identity provider in "state" (or RelayState) parameter
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdentityBrokerState {

    private static final Pattern DOT = Pattern.compile("\\.");


    public static IdentityBrokerState decoded(String state, String clientId, String clientClientId, String tabId, String clientData) {

        String clientIdEncoded = clientClientId; // Default use the client.clientId
        boolean isUuid = false;
        if (clientId != null) {
            // According to (http://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf) there is a limit on the relaystate of 80 bytes.
            // in order to try to adher to the SAML specification we use an encoded value of the client.id (probably UUID) instead of the with
            // probability bigger client.clientId. If the client.id is not in UUID format we just use the client.clientid as is
            try {
                UUID clientDbUuid = UUID.fromString(clientId);
                ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
                bb.putLong(clientDbUuid.getMostSignificantBits());
                bb.putLong(clientDbUuid.getLeastSignificantBits());
                byte[] clientUuidBytes = bb.array();
                clientIdEncoded = Base64Url.encode(clientUuidBytes);
                isUuid = true;
            } catch (RuntimeException e) {
                // Ignore...the clientid in the database was not in UUID format. Just use as is.
            }
        }
        if (!isUuid && clientIdEncoded != null) {
            clientIdEncoded = Base64Url.encode(clientIdEncoded.getBytes(StandardCharsets.UTF_8));
        }
        String encodedState = state + "." + tabId + "." + clientIdEncoded;
        if (clientData != null) {
            encodedState = encodedState + "." + clientData;
        }

        return new IdentityBrokerState(state, clientClientId, tabId, clientData, encodedState);
    }


    public static IdentityBrokerState encoded(String encodedState, RealmModel realmModel) {
        String[] decoded = DOT.split(encodedState, 4);

        String state =(decoded.length > 0) ? decoded[0] : null;
        String tabId = (decoded.length > 1) ? decoded[1] : null;
        String clientId = (decoded.length > 2) ? decoded[2] : null;
        String clientData = (decoded.length > 3) ? decoded[3] : null;
        boolean isUuid = false;

        if (clientId != null) {
            try {
                // If this decoding succeeds it was the result of the encoding of a UUID client.id - if it fails we interpret it as client.clientId
                // in accordance to the method decoded above
                byte[] decodedClientId = Base64Url.decode(clientId);
                ByteBuffer bb = ByteBuffer.wrap(decodedClientId);
                long first = bb.getLong();
                long second = bb.getLong();
                UUID clientDbUuid = new UUID(first, second);
                String clientIdInDb = clientDbUuid.toString();
                ClientModel clientModel = realmModel.getClientById(clientIdInDb);
                if (clientModel != null) {
                    clientId = clientModel.getClientId();
                    isUuid = true;
                }
            } catch (RuntimeException e) {
                // Ignore...the clientid was not in encoded uuid format. Just use as it is.
            }
            if (!isUuid) {
                clientId = new String(Base64Url.decode(clientId), StandardCharsets.UTF_8);
            }
        }

        return new IdentityBrokerState(state, clientId, tabId, clientData, encodedState);
    }



    private final String decodedState;
    private final String clientId;
    private final String tabId;
    private final String clientData;

    // Encoded form of whole state
    private final String encoded;

    private IdentityBrokerState(String decodedStateParam, String clientId, String tabId, String clientData, String encoded) {
        this.decodedState = decodedStateParam;
        this.clientId = clientId;
        this.tabId = tabId;
        this.clientData = clientData;
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

    public String getClientData() {
        return clientData;
    }

    public String getEncoded() {
        return encoded;
    }
}
