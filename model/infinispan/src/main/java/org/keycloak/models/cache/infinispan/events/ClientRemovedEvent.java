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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.RealmCacheManager;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(ClientRemovedEvent.ExternalizerImpl.class)
public class ClientRemovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String clientUuid;
    private String clientId;
    private String realmId;
    // roleId -> roleName
    private Map<String, String> clientRoles;
    private static final Logger log = Logger.getLogger(ClientRemovedEvent.class);

    public static ClientRemovedEvent create(ClientModel client) {
        log.tracev("Created; clientId={0}", client.getClientId());

        ClientRemovedEvent event = new ClientRemovedEvent();

        event.realmId = client.getRealm().getId();
        event.clientUuid = client.getId();
        event.clientId = client.getClientId();
        event.clientRoles = client.getRolesStream().collect(Collectors.toMap(RoleModel::getId, RoleModel::getName));

        return event;
    }

    @Override
    protected void finalize() throws Throwable {
        log.tracev("Finalized; clientId={0}", clientId);
        super.finalize();
    }

    @Override
    public String getId() {
        return clientUuid;
    }

    @Override
    public String toString() {
        return String.format("ClientRemovedEvent [ realmId=%s, clientUuid=%s, clientId=%s, clientRoleIds=%s ]", realmId, clientUuid, clientId, clientRoles);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.clientRemoval(realmId, clientUuid, clientId, invalidations);

        // Separate iteration for all client roles to invalidate records dependent on them
        for (Map.Entry<String, String> clientRole : clientRoles.entrySet()) {
            String roleId = clientRole.getKey();
            String roleName = clientRole.getValue();
            realmCache.roleRemoval(roleId, roleName, clientUuid, invalidations);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClientRemovedEvent that = (ClientRemovedEvent) o;
        boolean equals = Objects.equals(clientUuid, that.clientUuid) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(realmId, that.realmId) &&
                Objects.equals(clientRoles, that.clientRoles);
        log.tracev("Equals; clientId={0}, equals={1}", clientId, equals);
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientUuid, clientId, realmId, clientRoles);
    }

    public static class ExternalizerImpl implements Externalizer<ClientRemovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ClientRemovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.clientUuid, output);
            MarshallUtil.marshallString(obj.clientId, output);
            MarshallUtil.marshallString(obj.realmId, output);
            KeycloakMarshallUtil.writeMap(obj.clientRoles, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);

            log.tracev("Write; clientId={0}", obj.clientId);
        }

        @Override
        public ClientRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ClientRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ClientRemovedEvent res = new ClientRemovedEvent();
            res.clientUuid = MarshallUtil.unmarshallString(input);
            res.clientId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);
            res.clientRoles = KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT,
              size -> new ConcurrentHashMap<>(size));

            log.tracev("Read; clientId={0}", res.clientId);

            return res;
        }
    }
}
