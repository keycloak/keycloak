/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sessions;

import java.util.regex.Pattern;

/**
 * Allow to encode compound string to fully lookup authenticationSessionModel
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionCompoundId {

    private static final Pattern DOT = Pattern.compile("\\.");

    public static AuthenticationSessionCompoundId fromAuthSession(AuthenticationSessionModel authSession) {
        return decoded(authSession.getParentSession().getId(), authSession.getTabId(), authSession.getClient().getId());
    }

    public static AuthenticationSessionCompoundId decoded(String rootAuthSessionId, String tabId, String clientUUID) {
        String encodedId = rootAuthSessionId + "." + tabId + "." + clientUUID;
        return new AuthenticationSessionCompoundId(rootAuthSessionId, tabId, clientUUID, encodedId);
    }

    public static AuthenticationSessionCompoundId encoded(String encodedId) {
        String[] decoded = DOT.split(encodedId, 3);

        String rootAuthSessionId =(decoded.length > 0) ? decoded[0] : null;
        String tabId = (decoded.length > 1) ? decoded[1] : null;
        String clientUUID = (decoded.length > 2) ? decoded[2] : null;

        return new AuthenticationSessionCompoundId(rootAuthSessionId, tabId, clientUUID, encodedId);
    }



    private final String rootSessionId;
    private final String tabId;
    private final String clientUUID;
    private final String encodedId;

    public AuthenticationSessionCompoundId(String rootSessionId, String tabId, String clientUUID, String encodedId) {
        this.rootSessionId = rootSessionId;
        this.tabId = tabId;
        this.clientUUID = clientUUID;
        this.encodedId = encodedId;
    }

    public String getRootSessionId() {
        return rootSessionId;
    }

    public String getTabId() {
        return tabId;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public String getEncodedId() {
        return encodedId;
    }
}
