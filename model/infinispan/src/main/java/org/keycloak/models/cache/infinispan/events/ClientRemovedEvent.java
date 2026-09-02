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
import java.util.stream.Collectors;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.RealmCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.CLIENT_REMOVED_EVENT)
public class ClientRemovedEvent extends BaseClientEvent {

    @ProtoField(3)
    final String clientId;
    // roleId -> roleName
    @ProtoField(4)
    final Map<String, String> clientRoles;

    @ProtoFactory
    ClientRemovedEvent(String id, String realmId, String clientId, Map<String, String> clientRoles) {
        super(id, realmId);
        this.clientId = Objects.requireNonNull(clientId);
        this.clientRoles = Objects.requireNonNull(clientRoles);
    }


    public static ClientRemovedEvent create(ClientModel client) {
        var clientRoles = client.getRolesStream().collect(Collectors.toMap(RoleModel::getId, RoleModel::getName));
        return new ClientRemovedEvent(client.getId(), client.getRealm().getId(), client.getClientId(), clientRoles);
    }


    @Override
    public String toString() {
        return String.format("ClientRemovedEvent [ realmId=%s, clientUuid=%s, clientId=%s, clientRoleIds=%s ]", realmId, getId(), clientId, clientRoles);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.clientRemoval(realmId, getId(), clientId, invalidations);

        // Separate iteration for all client roles to invalidate records dependent on them
        for (Map.Entry<String, String> clientRole : clientRoles.entrySet()) {
            String roleId = clientRole.getKey();
            String roleName = clientRole.getValue();
            realmCache.roleRemoval(roleId, roleName, getId(), invalidations);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClientRemovedEvent that = (ClientRemovedEvent) o;
        return clientId.equals(that.clientId) &&
                clientRoles.equals(that.clientRoles);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + clientId.hashCode();
        result = 31 * result + clientRoles.hashCode();
        return result;
    }
}
