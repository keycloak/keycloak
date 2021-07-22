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

package org.keycloak.testsuite.theme;

import org.junit.After;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.List;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractEmailSubjectTemplateTest extends AbstractTestRealmKeycloakTest {

    public String testUserUsername = "mail-subject-test";
    public String testUserEmail = "mail-subject-test@test.com";
    public String testUserPassword = "password";

    public String adminUsername = "admin";
    public String adminEmail = "admin@localhost";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserBuilder user = UserBuilder.create()
                .username(testUserUsername)
                .enabled(true)
                .email(testUserEmail)
                .role("account", "manage-account")
                .password(testUserPassword);
        RealmBuilder.edit(testRealm).user(user);

        RealmResource realm = adminClient.realm("master");
        List<UserRepresentation> adminUser = realm.users().search(adminUsername, 0, 1);
        UserRepresentation adminRep = UserBuilder.edit(adminUser.get(0)).email(adminEmail).build();
        realm.users().get(adminRep.getId()).update(adminRep);
    }

    /**
     * Remove cookies at the end so that the next test will start out
     * using the default locale.
     */
    @After
    public void deleteCookies() {
        driver.manage().deleteAllCookies();
    }
}
