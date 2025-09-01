/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.models.policy.conditions;

import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.ResourcePolicyConditionProvider;
import org.keycloak.models.policy.ResourcePolicyEvent;
import org.keycloak.models.policy.ResourceType;

public class GroupMembershipPolicyConditionProvider implements ResourcePolicyConditionProvider {

    private final List<String> expectedGroups;
    private final KeycloakSession session;

    public GroupMembershipPolicyConditionProvider(KeycloakSession session, List<String> expectedGroups) {
        this.session = session;
        this.expectedGroups = expectedGroups;;
    }

    @Override
    public boolean evaluate(ResourcePolicyEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);

        for (String expectedGroup : expectedGroups) {
            GroupModel group = session.groups().getGroupById(realm, expectedGroup);

            if (user.isMemberOf(group)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() {

    }
}
