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

package org.keycloak.services.managers;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class AuthSessionId {

    // Decoded ID of authenticationSession WITHOUT route attached (EG. "5e161e00-d426-4ea6-98e9-52eb9844e2d7")
    private final String decodedId;

    // Encoded ID of authenticationSession WITH route attached (EG. "5e161e00-d426-4ea6-98e9-52eb9844e2d7.node1")
    private final String encodedId;

    AuthSessionId(String decodedId, String encodedId) {
        this.decodedId = decodedId;
        this.encodedId = encodedId;
    }


    public String getDecodedId() {
        return decodedId;
    }

    public String getEncodedId() {
        return encodedId;
    }
}
