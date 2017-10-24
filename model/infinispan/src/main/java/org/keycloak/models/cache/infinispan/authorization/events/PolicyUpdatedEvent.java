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

package org.keycloak.models.cache.infinispan.authorization.events;

import org.keycloak.models.cache.infinispan.authorization.StoreFactoryCacheManager;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(PolicyUpdatedEvent.ExternalizerImpl.class)
public class PolicyUpdatedEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    private String id;
    private String name;
    private Set<String> resources;
    private Set<String> resourceTypes;
    private Set<String> scopes;
    private String serverId;

    public static PolicyUpdatedEvent create(String id, String name, Set<String> resources, Set<String> resourceTypes, Set<String> scopes, String serverId) {
        PolicyUpdatedEvent event = new PolicyUpdatedEvent();
        event.id = id;
        event.name = name;
        event.resources = resources;
        event.resourceTypes = resourceTypes;
        event.scopes = scopes;
        event.serverId = serverId;
        return event;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("PolicyUpdatedEvent [ id=%s, name=%s ]", id, name);
    }

    @Override
    public void addInvalidations(StoreFactoryCacheManager cache, Set<String> invalidations) {
        cache.policyUpdated(id, name, resources, resourceTypes, scopes, serverId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<PolicyUpdatedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, PolicyUpdatedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.id, output);
            MarshallUtil.marshallString(obj.name, output);
            KeycloakMarshallUtil.writeCollection(obj.resources, KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeCollection(obj.resourceTypes, KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeCollection(obj.scopes, KeycloakMarshallUtil.STRING_EXT, output);
            MarshallUtil.marshallString(obj.serverId, output);
        }

        @Override
        public PolicyUpdatedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public PolicyUpdatedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            PolicyUpdatedEvent res = new PolicyUpdatedEvent();
            res.id = MarshallUtil.unmarshallString(input);
            res.name = MarshallUtil.unmarshallString(input);
            res.resources = KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, HashSet::new);
            res.resourceTypes = KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, HashSet::new);
            res.scopes = KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, HashSet::new);
            res.serverId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
