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
package org.keycloak.testsuite.admin.event;

import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.TestCleanup;

import java.util.Collections;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractEventTest extends AbstractAuthTest {

    protected RealmEventsConfigRepresentation configRep;

    @Before
    public void setConfigRep() {
        RealmResource testRsc = testRealmResource();
        configRep = testRsc.getRealmEventsConfig();
        configRep.setAdminEventsDetailsEnabled(false);
        configRep.setAdminEventsEnabled(false);
        configRep.setEventsEnabled(false);
        configRep.setEnabledEventTypes(Collections.<String>emptyList()); // resets to all types
        saveConfig();
    }

    @Override
    public void setDefaultPageUriParameters() {
        testRealmPage.setAuthRealm("test");
        accountPage.setAuthRealm("test");
    }

    protected void saveConfig() {
        RealmResource testRsc = testRealmResource();
        testRsc.updateRealmEventsConfig(configRep);
        configRep = testRsc.getRealmEventsConfig();
    }

    protected void enableEvents() {
        configRep.setEventsEnabled(true);
        configRep.setAdminEventsEnabled(Boolean.TRUE);

        saveConfig();
    }

    protected String realmName() {
        return testRealmResource().toRepresentation().getId();
    }
}
