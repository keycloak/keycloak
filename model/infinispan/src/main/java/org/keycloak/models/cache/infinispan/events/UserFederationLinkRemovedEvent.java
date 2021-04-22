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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.cache.infinispan.UserCacheManager;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(UserFederationLinkRemovedEvent.ExternalizerImpl.class)
public class UserFederationLinkRemovedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private String userId;
    private String realmId;
    private String identityProviderId;
    private String socialUserId;

    public static UserFederationLinkRemovedEvent create(String userId, String realmId, FederatedIdentityModel socialLink) {
        UserFederationLinkRemovedEvent event = new UserFederationLinkRemovedEvent();
        event.userId = userId;
        event.realmId = realmId;
        if (socialLink != null) {
            event.identityProviderId = socialLink.getIdentityProvider();
            event.socialUserId = socialLink.getUserId();
        }
        return event;
    }

    @Override
    public String getId() {
        return userId;
    }

    public String getRealmId() {
        return realmId;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public String getSocialUserId() {
        return socialUserId;
    }

    @Override
    public String toString() {
        return String.format("UserFederationLinkRemovedEvent [ userId=%s, identityProviderId=%s, socialUserId=%s ]", userId, identityProviderId, socialUserId);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.federatedIdentityLinkRemovedInvalidation(userId, realmId, identityProviderId, socialUserId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserFederationLinkRemovedEvent that = (UserFederationLinkRemovedEvent) o;
        return Objects.equals(userId, that.userId) && Objects.equals(realmId, that.realmId) && Objects.equals(identityProviderId, that.identityProviderId) && Objects.equals(socialUserId, that.socialUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, realmId, identityProviderId, socialUserId);
    }

    public static class ExternalizerImpl implements Externalizer<UserFederationLinkRemovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, UserFederationLinkRemovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.userId, output);
            MarshallUtil.marshallString(obj.realmId, output);
            MarshallUtil.marshallString(obj.identityProviderId, output);
            MarshallUtil.marshallString(obj.socialUserId, output);
        }

        @Override
        public UserFederationLinkRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserFederationLinkRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserFederationLinkRemovedEvent res = new UserFederationLinkRemovedEvent();
            res.userId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);
            res.identityProviderId = MarshallUtil.unmarshallString(input);
            res.socialUserId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
