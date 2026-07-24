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

package org.keycloak.tests.admin.group;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public abstract class AbstractGroupTest {

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectOAuthClient
    OAuthClient oAuth;

    AccessToken login(String username, String clientId, String clientSecret) {
        AccessTokenResponse tokenResponse = oAuth.client(clientId, clientSecret).doPasswordGrantRequest(username, "password");
        return oAuth.parseToken(tokenResponse.getAccessToken(), AccessToken.class);
    }

    String createGroup(ManagedRealm managedRealm, GroupRepresentation group) {
        Response response = managedRealm.admin().groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.groups().group(groupId).remove());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupPath(groupId), group, ResourceType.GROUP);

        // Set ID to the original rep
        group.setId(groupId);
        return groupId;
    }

    void addSubGroup(ManagedRealm managedRealm, GroupRepresentation parent, GroupRepresentation child) {
        Response response = managedRealm.admin().groups().add(child);
        String childUuid = ApiUtil.getCreatedId(response);
        child.setId(childUuid);
        response = managedRealm.admin().groups().group(parent.getId()).subGroup(child);
        response.close();
    }

    RoleRepresentation createRealmRole(ManagedRealm managedRealm, RoleRepresentation role) {
        managedRealm.admin().roles().create(role);
        RoleRepresentation created = managedRealm.admin().roles().get(role.getName()).toRepresentation();
        String createdName = created.getName();
        managedRealm.cleanup().add(r -> r.roles().deleteRole(createdName));
        return created;
    }
}
