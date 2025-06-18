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
package org.keycloak.saml.processing.core.saml.v2.holders;

/**
 * Holder containing the information about a destination
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jul 24, 2009
 */
public class DestinationInfoHolder {

    private String destination;
    private String samlMessage;
    private String relayState;

    /**
     * Create an holder
     *
     * @param destination The destination where the post will be sent
     * @param samlMessage SAML Message
     * @param relayState
     */
    public DestinationInfoHolder(String destination, String samlMessage, String relayState) {
        this.destination = destination;
        this.samlMessage = samlMessage;
        this.relayState = relayState;
    }

    public String getDestination() {
        return destination;
    }

    public String getSamlMessage() {
        return samlMessage;
    }

    public String getRelayState() {
        return relayState;
    }
}
