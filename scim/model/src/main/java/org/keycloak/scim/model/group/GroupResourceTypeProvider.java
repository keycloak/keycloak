/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;

public class GroupResourceTypeProvider extends AbstractScimResourceTypeProvider<GroupModel, Group> {

    public GroupResourceTypeProvider(KeycloakSession session) {
        super(session, new GroupCoreModelSchema());
    }

    @Override
    public Group onCreate(Group group) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel model = session.groups().createGroup(realm, group.getDisplayName());
        populate(model, group);
        return group;
    }

    @Override
    protected Group onUpdate(GroupModel model, Group resource) {
        return resource;
    }

    @Override
    protected GroupModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().getGroupById(realm, id);
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
    }

    @Override
    protected Stream<GroupModel> getModels() {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().getTopLevelGroupsStream(realm);
    }

    @Override
    public boolean onDelete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().removeGroup(realm, getModel(id));
    }

    @Override
    public Class<Group> getResourceType() {
        return Group.class;
    }

    @Override
    public void close() {

    }
}
