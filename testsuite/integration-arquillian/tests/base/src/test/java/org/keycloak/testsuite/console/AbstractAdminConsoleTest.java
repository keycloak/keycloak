/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console;

import java.util.List;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.Realm;
import static org.keycloak.testsuite.console.page.Realm.MASTER;
import static org.keycloak.testsuite.console.page.Realm.TEST;

/**
 *
 * @author Petr Mensik
 * @param <P>
 */
public abstract class AbstractAdminConsoleTest<P extends AdminConsole> extends AbstractKeycloakTest {

    @Page
    protected P page;

    @Page
    protected Realm testRealm;

    @Before
    public void beforeConsoleTest() {
        loginAsAdmin();
    }

    @After
    public void afterConsoleTest() {
        adminConsole.setAdminRealm(MASTER);
        logOut();
    }

    @Override
    public void setPageUriTemplateValues() {
        super.setPageUriTemplateValues();
        testRealm.setAdminRealm(MASTER);
        testRealm.setConsoleRealm(TEST);
    }

    @Override
    public void loadTestRealmsInto(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

}
