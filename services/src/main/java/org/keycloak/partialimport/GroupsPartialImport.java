/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.partialimport;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

import java.util.List;

/**
 * Partial import handler for Groups.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class GroupsPartialImport extends AbstractPartialImport<GroupRepresentation> {

    @Override
    public List<GroupRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getGroups();
    }

    @Override
    public String getName(GroupRepresentation group) {
        return group.getName();
    }

    private GroupModel findGroupModel(RealmModel realm, GroupRepresentation groupRep) {
        return KeycloakModelUtils.findGroupByPath(realm, groupRep.getPath());
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, GroupRepresentation groupRep) {
        return findGroupModel(realm, groupRep).getId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, GroupRepresentation groupRep) {
        return findGroupModel(realm, groupRep) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, GroupRepresentation groupRep) {
        return "Group '" + groupRep.getPath() + "' already exists";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.GROUP;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, GroupRepresentation groupRep) {
        GroupModel group = realm.getGroupById(getModelId(realm, session, groupRep));
        realm.removeGroup(group);
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, GroupRepresentation groupRep) {
        RepresentationToModel.importGroup(realm, null, groupRep);
    }

}
