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

package org.keycloak.tests.admin.authentication;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.tests.admin.AbstractPermissionsTest;

import org.junit.jupiter.api.BeforeEach;

public class AbstractPolicyTest extends AbstractPermissionsTest {

    @InjectRealm(config = TestRealm.class, ref = "policy-test")
    ManagedRealm managedRealm;
    AuthenticationManagementResource authMgmtResource;

    @InjectAdminEvents(realmRef = "policy-test")
    AdminEvents adminEvents;

    @BeforeEach
    public void before() {
        authMgmtResource = managedRealm.admin().flows();
    }

    protected static class TestRealm extends PermissionsTestRealmConfig1 {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm)
                    .adminPermissionsEnabled(true)
                    .adminEventsEnabled(true)
                    .adminEventsDetailsEnabled(true);
        }
    }
}
