/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.partialimport;

import java.util.List;

import org.keycloak.events.hooks.EventHookStoreProvider;
import org.keycloak.events.hooks.EventHookTargetModel;
import org.keycloak.events.hooks.EventHookTargetRepresentationUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookTargetRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

import static org.keycloak.common.util.Time.currentTimeMillis;

public class EventHookTargetsPartialImport extends AbstractPartialImport<EventHookTargetRepresentation> {

    @Override
    public List<EventHookTargetRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getEventHookTargets();
    }

    @Override
    public String getName(EventHookTargetRepresentation resourceRep) {
        return resourceRep.getName();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, EventHookTargetRepresentation resourceRep) {
        EventHookTargetModel target = findExistingTarget(realm, session, resourceRep);
        return target == null ? null : target.getId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, EventHookTargetRepresentation resourceRep) {
        return findExistingTarget(realm, session, resourceRep) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, EventHookTargetRepresentation resourceRep) {
        return "Event hook target '" + getName(resourceRep) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.EVENT_HOOK_TARGET;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, EventHookTargetRepresentation resourceRep) {
        EventHookTargetModel target = findExistingTarget(realm, session, resourceRep);
        if (target != null) {
            session.getProvider(EventHookStoreProvider.class).deleteTarget(realm.getId(), target.getId());
        }
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, EventHookTargetRepresentation resourceRep) {
        session.getProvider(EventHookStoreProvider.class).createTarget(
                EventHookTargetRepresentationUtil.toModel(session, realm, resourceRep, null, currentTimeMillis(), true, true));
    }

    private EventHookTargetModel findExistingTarget(RealmModel realm, KeycloakSession session, EventHookTargetRepresentation representation) {
        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        if (representation.getId() != null && !representation.getId().isBlank()) {
            EventHookTargetModel byId = store.getTarget(realm.getId(), representation.getId());
            if (byId != null) {
                return byId;
            }
        }

        return store.getTargetsStream(realm.getId())
                .filter(target -> target.getName() != null && target.getName().equals(representation.getName()))
                .findFirst()
                .orElse(null);
    }
}
