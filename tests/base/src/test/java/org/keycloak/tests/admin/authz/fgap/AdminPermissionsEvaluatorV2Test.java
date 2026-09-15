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

package org.keycloak.tests.admin.authz.fgap;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminPermissionsEvaluatorV2Test {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @Test
    public void testCreateEvaluatorForUser() {
        String userId;
        try (Response response = realm.admin().users().create(
                UserBuilder.create().username("test-user").enabled(true).build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        String realmName = realm.getName();
        String testUserId = userId;
        runOnServer.run((RunOnServer) session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realmModel);
            UserModel user = session.users().getUserById(realmModel, testUserId);
            var evaluator = AdminPermissions.evaluator(session, realmModel, realmModel, user);
            Assert.assertFalse(evaluator.isRealmAdmin());
            Assert.assertFalse(evaluator.hasOneAdminRole(AdminRoles.ALL_REALM_ROLES));
        });
    }
}