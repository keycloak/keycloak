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

package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.UserCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.stream.InRealmPredicate;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserCacheManager extends CacheManager {

    private static final Logger logger = Logger.getLogger(UserCacheManager.class);

    protected volatile boolean enabled = true;

    public UserCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        super(cache, revisions);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void clear() {
        cache.clear();
        revisions.clear();
    }


    public void userUpdatedInvalidations(String userId, String username, String email, String realmId, Set<String> invalidations) {
        invalidations.add(userId);
        if (email != null) invalidations.add(UserCacheSession.getUserByEmailCacheKey(realmId, email));
        invalidations.add(UserCacheSession.getUserByUsernameCacheKey(realmId, username));
    }

    // Fully invalidate user including consents and federatedIdentity links.
    public void fullUserInvalidation(String userId, String username, String email, String realmId, boolean identityFederationEnabled, Map<String, String> federatedIdentities, Set<String> invalidations) {
        userUpdatedInvalidations(userId, username, email, realmId, invalidations);

        if (identityFederationEnabled) {
            // Invalidate all keys for lookup this user by any identityProvider link
            for (Map.Entry<String, String> socialLink : federatedIdentities.entrySet()) {
                String fedIdentityCacheKey = UserCacheSession.getUserByFederatedIdentityCacheKey(realmId, socialLink.getKey(), socialLink.getValue());
                invalidations.add(fedIdentityCacheKey);
            }

            // Invalidate federationLinks of user
            invalidations.add(UserCacheSession.getFederatedIdentityLinksCacheKey(userId));
        }

        // Consents
        invalidations.add(UserCacheSession.getConsentCacheKey(userId));
    }

    public void federatedIdentityLinkUpdatedInvalidation(String userId, Set<String> invalidations) {
        invalidations.add(UserCacheSession.getFederatedIdentityLinksCacheKey(userId));
    }

    public void federatedIdentityLinkRemovedInvalidation(String userId, String realmId, String identityProviderId, String socialUserId, Set<String> invalidations) {
        invalidations.add(UserCacheSession.getFederatedIdentityLinksCacheKey(userId));
        if (identityProviderId != null) {
            invalidations.add(UserCacheSession.getUserByFederatedIdentityCacheKey(realmId, identityProviderId, socialUserId));
        }
    }

    public void consentInvalidation(String userId, Set<String> invalidations) {
        invalidations.add(UserCacheSession.getConsentCacheKey(userId));
    }


    @Override
    protected void addInvalidationsFromEvent(InvalidationEvent event, Set<String> invalidations) {
        ((UserCacheInvalidationEvent) event).addInvalidations(this, invalidations);
    }

    public void invalidateRealmUsers(String realm, Set<String> invalidations) {
        InRealmPredicate inRealmPredicate = getInRealmPredicate(realm);
        addInvalidations(inRealmPredicate, invalidations);
    }

    private InRealmPredicate getInRealmPredicate(String realmId) {
        return InRealmPredicate.create().realm(realmId);
    }
}
