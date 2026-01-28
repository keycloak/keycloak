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

import java.util.Objects;
import java.util.Set;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.USER_FEDERATION_LINK_REMOVED_EVENT)
public class UserFederationLinkRemovedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    final String realmId;
    final String identityProviderId;
    final String socialUserId;

    private UserFederationLinkRemovedEvent(String id, String realmId, String identityProviderId, String socialUserId) {
        super(id);
        this.realmId = Objects.requireNonNull(realmId);
        // may be null
        this.identityProviderId = identityProviderId;
        this.socialUserId = socialUserId;
    }

    public static UserFederationLinkRemovedEvent create(String userId, String realmId, FederatedIdentityModel socialLink) {
        String identityProviderId = socialLink == null ? null : socialLink.getIdentityProvider();
        String socialUserId = socialLink == null ? null : socialLink.getUserId();
        return new UserFederationLinkRemovedEvent(userId, realmId, identityProviderId, socialUserId);
    }

    @ProtoFactory
    static UserFederationLinkRemovedEvent protoFactory(String id, String realmId, String identityProviderId, String socialUserId) {
        return new UserFederationLinkRemovedEvent(id, realmId, identityProviderId, socialUserId);
    }

    @ProtoField(2)
    public String getRealmId() {
        return realmId;
    }

    @ProtoField(3)
    public String getIdentityProviderId() {
        return identityProviderId;
    }

    @ProtoField(4)
    public String getSocialUserId() {
        return socialUserId;
    }

    @Override
    public String toString() {
        return String.format("UserFederationLinkRemovedEvent [ userId=%s, identityProviderId=%s, socialUserId=%s ]", getId(), identityProviderId, socialUserId);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.federatedIdentityLinkRemovedInvalidation(getId(), realmId, identityProviderId, socialUserId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserFederationLinkRemovedEvent that = (UserFederationLinkRemovedEvent) o;
        return Objects.equals(realmId, that.realmId) && Objects.equals(identityProviderId, that.identityProviderId) && Objects.equals(socialUserId, that.socialUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), realmId, identityProviderId, socialUserId);
    }

}
