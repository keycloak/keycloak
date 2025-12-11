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

package org.keycloak.testsuite.events;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.client.resources.TestingResource;
import org.keycloak.testsuite.util.RealmBuilder;

import org.junit.Before;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractEventsTest extends AbstractKeycloakTest {

    public static final String REALM_NAME_1 = "realmName1";
    public static final String REALM_NAME_2 = "realmName2";

    protected String realmId;
    protected String realmId2;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME_1).build());
        testRealms.add(RealmBuilder.create().name(REALM_NAME_2).build());
    }

    @Before
    public void before() {
        realmId = adminClient.realm(REALM_NAME_1).toRepresentation().getId();
        realmId2 = adminClient.realm(REALM_NAME_2).toRepresentation().getId();
    }

    protected TestingResource testing() {
        return getTestingClient().testing();
    }

    protected List<String> toList(Enum... enumTypes) {
        List<String> enumList = new ArrayList<>();
        for (Enum type : enumTypes) {
            enumList.add(type.toString());
        }

        return enumList;
    }
}
