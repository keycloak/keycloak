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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.cache.infinispan.UserCacheManager;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * Used when user added/removed
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(UserFullInvalidationEvent.ExternalizerImpl.class)
public class UserFullInvalidationEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private String userId;
    private String username;
    private String email;
    private String realmId;
    private boolean identityFederationEnabled;
    private Map<String, String> federatedIdentities;

    public static UserFullInvalidationEvent create(String userId, String username, String email, String realmId, boolean identityFederationEnabled, Stream<FederatedIdentityModel> federatedIdentities) {
        UserFullInvalidationEvent event = new UserFullInvalidationEvent();
        event.userId = userId;
        event.username = username;
        event.email = email;
        event.realmId = realmId;

        event.identityFederationEnabled = identityFederationEnabled;
        if (identityFederationEnabled) {
            event.federatedIdentities = federatedIdentities.collect(Collectors.toMap(socialLink -> socialLink.getIdentityProvider(),
                    socialLink -> socialLink.getUserId()));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserFullInvalidationEvent that = (UserFullInvalidationEvent) o;
        return identityFederationEnabled == that.identityFederationEnabled && Objects.equals(userId, that.userId) && Objects.equals(username, that.username) && Objects.equals(email, that.email) && Objects.equals(realmId, that.realmId) && Objects.equals(federatedIdentities, that.federatedIdentities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, username, email, realmId, identityFederationEnabled, federatedIdentities);
    }

    public static class ExternalizerImpl implements Externalizer<UserFullInvalidationEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, UserFullInvalidationEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.userId, output);
            MarshallUtil.marshallString(obj.username, output);
            MarshallUtil.marshallString(obj.email, output);
            MarshallUtil.marshallString(obj.realmId, output);
            output.writeBoolean(obj.identityFederationEnabled);
            KeycloakMarshallUtil.writeMap(obj.federatedIdentities, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);
        }

        @Override
        public UserFullInvalidationEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserFullInvalidationEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserFullInvalidationEvent res = new UserFullInvalidationEvent();
            res.userId = MarshallUtil.unmarshallString(input);
            res.username = MarshallUtil.unmarshallString(input);
            res.email = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);
            res.identityFederationEnabled = input.readBoolean();
            res.federatedIdentities = KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, HashMap::new);

            return res;
        }
    }
}
