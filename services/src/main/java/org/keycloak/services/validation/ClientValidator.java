/*
 *
 *  * Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.services.validation;

import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientValidator {
    /**
     * Checks if the Client's Redirect URIs doesn't contain any URI fragments (like http://example.org/auth#fragment)
     *
     * @see <a href="https://issues.jboss.org/browse/KEYCLOAK-3421">KEYCLOAK-3421</a>
     * @param client
     * @param messages
     * @return true if Redirect URIs doesn't contain any URI with fragments
     */
    public static boolean validate(ClientRepresentation client, ValidationMessages messages) {
        boolean isValid = true;

        if (client.getRedirectUris() != null) {
            long urisWithFragmentCount = client.getRedirectUris().stream().filter(p -> p.contains("#")).count();
            if (urisWithFragmentCount > 0) {
                messages.add("redirectUris", "Redirect URIs must not contain an URI fragment", "clientRedirectURIsFragmentError");
                isValid = false;
            }
        }

        if (client.getRootUrl() != null && client.getRootUrl().contains("#")) {
            messages.add("rootUrl", "Root URL must not contain an URL fragment", "clientRootURLFragmentError");
            isValid = false;
        }

        return isValid;
    }
}
