/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.representations.account;

import java.util.List;

public class ConsentRepresentation {

    private List<ConsentScopeRepresentation> grantedScopes;

    private Long createdDate;

    private Long lastUpdatedDate;

    public ConsentRepresentation() {
    }

    public ConsentRepresentation(List<ConsentScopeRepresentation> grantedScopes, Long createdDate, Long lastUpdatedDate) {
        this.grantedScopes = grantedScopes;
        this.createdDate = createdDate;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public List<ConsentScopeRepresentation> getGrantedScopes() {
        return grantedScopes;
    }

    public void setGrantedScopes(List<ConsentScopeRepresentation> grantedScopes) {
        this.grantedScopes = grantedScopes;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
