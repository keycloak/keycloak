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

package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentRepresentation {

    protected String clientId;

    // Key is protocol, Value is list of granted consents for this protocol
    protected Map<String, List<String>> grantedProtocolMappers;

    protected List<String> grantedRealmRoles;

    // Key is clientId, Value is list of granted roles of this client
    protected Map<String, List<String>> grantedClientRoles;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, List<String>> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Map<String, List<String>> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }

    public List<String> getGrantedRealmRoles() {
        return grantedRealmRoles;
    }

    public void setGrantedRealmRoles(List<String> grantedRealmRoles) {
        this.grantedRealmRoles = grantedRealmRoles;
    }

    public Map<String, List<String>> getGrantedClientRoles() {
        return grantedClientRoles;
    }

    public void setGrantedClientRoles(Map<String, List<String>> grantedClientRoles) {
        this.grantedClientRoles = grantedClientRoles;
    }
}
