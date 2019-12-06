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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.UserConsentModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CachedUserConsent {

    private final String clientDbId;
    private final Set<String> clientScopeIds = new HashSet<>();
    private final Long createdDate;
    private final Long lastUpdatedDate;

    public CachedUserConsent(UserConsentModel consentModel) {
        this.clientDbId = consentModel.getClient().getId();
        for (ClientScopeModel clientScope : consentModel.getGrantedClientScopes()) {
            this.clientScopeIds.add(clientScope.getId());
        }
        this.createdDate = consentModel.getCreatedDate();
        this.lastUpdatedDate = consentModel.getLastUpdatedDate();
    }

    public String getClientDbId() {
        return clientDbId;
    }

    public Set<String> getClientScopeIds() {
        return clientScopeIds;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }
}
