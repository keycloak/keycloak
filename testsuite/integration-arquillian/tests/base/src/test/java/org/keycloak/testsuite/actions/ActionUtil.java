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

package org.keycloak.testsuite.actions;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.UserBuilder;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ActionUtil {

    public static UserRepresentation findUserInRealmRep(RealmRepresentation testRealm, String username) {
        for (UserRepresentation rep : testRealm.getUsers()) {
            if (rep.getUsername().equals(username)) return rep;
        }

        return null;
    }

    public static UserRepresentation findUserWithAdminClient(Keycloak adminClient, String username) {
        return adminClient.realm("test").users().search(username, null, null, null, 0, 1).get(0);
    }

    public static void addRequiredActionForUser(RealmRepresentation testRealm, String userName, String action) {
        UserRepresentation user = findUserInRealmRep(testRealm, userName);
        UserBuilder.edit(user).requiredAction(action);
    }

    public static void addRequiredActionForRealm(RealmRepresentation testRealm, String providerId) {
        List<RequiredActionProviderRepresentation> requiredActions = testRealm.getRequiredActions();
        if (requiredActions == null) requiredActions = new LinkedList();
        
        RequiredActionProviderRepresentation last = requiredActions.get(requiredActions.size() - 1);

        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(providerId);
        action.setProviderId(providerId);
        action.setEnabled(true);
        action.setDefaultAction(true);
        action.setPriority(last.getPriority() + 1);

        requiredActions.add(action);
        testRealm.setRequiredActions(requiredActions);
    }
}
