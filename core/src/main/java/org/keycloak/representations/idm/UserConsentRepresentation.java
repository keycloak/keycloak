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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class UserConsentRepresentation {

    protected String clientId;

    protected List<String> grantedClientScopes;

    private Long createdDate;

    private Long lastUpdatedDate;

    @Deprecated
    protected List<String> grantedRealmRoles;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getGrantedClientScopes() {
        return grantedClientScopes;
    }

    public void setGrantedClientScopes(List<String> grantedClientScopes) {
        this.grantedClientScopes = grantedClientScopes;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    @Deprecated
    public List<String> getGrantedRealmRoles() {
        return grantedRealmRoles;
    }
}
