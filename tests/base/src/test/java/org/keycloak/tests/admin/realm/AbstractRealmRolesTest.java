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

package org.keycloak.tests.admin.realm;

import java.util.List;
import java.util.Map;

import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AbstractRealmRolesTest {

    @InjectRealm(config = RealmRolesRealmConf.class)
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private static class RealmRolesRealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.addGroup("test-role-group").path("/test-role-group");
            builder.addUser("test-role-member").name("Test", "Role User").
                    email("test-role-member@test-role-member.com").roles("default-roles-default").emailVerified(true).requiredActions();
            builder.addClient("client-a").id("client-a").description("Client A");
            builder.addClient("client-c").id("client-c").description("Client C");
            builder.addRole("role-a").description("Role A").attributes(Map.of("role-a-attr-key1", List.of("role-a-attr-val1")));
            builder.addRole("role-b").description("Role B");
            builder.addClientRole("client-a", "role-c").description("Role C");
            builder.addRole("role-with-users").description("Role with users");
            builder.addRole("role-without-users").description("role-without-users");

            return builder;
        }
    }
}
