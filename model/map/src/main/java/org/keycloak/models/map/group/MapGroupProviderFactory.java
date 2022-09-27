/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.group;

import java.util.stream.Collectors;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.GROUP_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.GROUP_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.ROLE_BEFORE_REMOVE;

/**
 *
 * @author mhajas
 */
public class MapGroupProviderFactory extends AbstractMapProviderFactory<MapGroupProvider, MapGroupEntity, GroupModel> implements GroupProviderFactory<MapGroupProvider>, InvalidationHandler {

    public MapGroupProviderFactory() {
        super(GroupModel.class, MapGroupProvider.class);
    }

    @Override
    public MapGroupProvider createNew(KeycloakSession session) {
        return new MapGroupProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "Group provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            create(session).preRemove((RealmModel) params[0]);
        } else if (type == ROLE_BEFORE_REMOVE) {
            create(session).preRemove((RealmModel) params[0], (RoleModel) params[1]);
        } else if (type == GROUP_BEFORE_REMOVE) {
            RealmModel realm = (RealmModel) params[0];
            GroupModel group = (GroupModel) params[1];

            realm.removeDefaultGroup(group);
            group.getSubGroupsStream().collect(Collectors.toSet()).forEach(subGroup -> create(session).removeGroup(realm, subGroup));
        } else if (type == GROUP_AFTER_REMOVE) {
            session.getKeycloakSessionFactory().publish(new GroupModel.GroupRemovedEvent() {
                @Override public RealmModel getRealm() { return (RealmModel) params[0]; }
                @Override public GroupModel getGroup() { return (GroupModel) params[1]; }
                @Override public KeycloakSession getKeycloakSession() { return session; }
            });
        }
    }
}
