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

package org.keycloak.testsuite;

import java.util.List;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.RealmBuilder;

import org.junit.Before;
import org.junit.Rule;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthenticationTest extends AbstractKeycloakTest {

    static final String REALM_NAME = "test";

    RealmResource realmResource;
    AuthenticationManagementResource authMgmtResource;
    protected String testRealmId;

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Before
    public void before() {
        realmResource = adminClient.realms().realm(REALM_NAME);
        authMgmtResource = realmResource.flows();
        testRealmId = realmResource.toRepresentation().getId();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = RealmBuilder.create().name(REALM_NAME).testEventListener().build();
        testRealmRep.setId(REALM_NAME);
        testRealms.add(testRealmRep);
    }

    public static AuthenticationFlowRepresentation findFlowByAlias(String alias, List<AuthenticationFlowRepresentation> flows) {
        for (AuthenticationFlowRepresentation flow : flows) {
            if (alias.equals(flow.getAlias())) {
                return flow;
            }
        }
        return null;
    }
}
