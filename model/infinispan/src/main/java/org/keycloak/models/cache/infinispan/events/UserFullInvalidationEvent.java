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

package org.keycloak.models.cache.infinispan.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.cache.infinispan.UserCacheManager;

/**
 * Used when user added/removed
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFullInvalidationEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private String userId;
    private String username;
    private String email;
    private String realmId;
    private boolean identityFederationEnabled;
    private Map<String, String> federatedIdentities;

    public static UserFullInvalidationEvent create(String userId, String username, String email, String realmId, boolean identityFederationEnabled, Collection<FederatedIdentityModel> federatedIdentities) {
        UserFullInvalidationEvent event = new UserFullInvalidationEvent();
        event.userId = userId;
        event.username = username;
        event.email = email;
        event.realmId = realmId;

        event.identityFederationEnabled = identityFederationEnabled;
        if (identityFederationEnabled) {
            event.federatedIdentities = new HashMap<>();
            for (FederatedIdentityModel socialLink : federatedIdentities) {
                event.federatedIdentities.put(socialLink.getIdentityProvider(), socialLink.getUserId());
            }
        }

        return event;
    }

    @Override
    public String getId() {
        return userId;
    }

    public Map<String, String> getFederatedIdentities() {
        return federatedIdentities;
    }

    @Override
    public String toString() {
        return String.format("UserFullInvalidationEvent [ userId=%s, username=%s, email=%s ]", userId, username, email);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.fullUserInvalidation(userId, username, email, realmId, identityFederationEnabled, federatedIdentities, invalidations);
    }
}
