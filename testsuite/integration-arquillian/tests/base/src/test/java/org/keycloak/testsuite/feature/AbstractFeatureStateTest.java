/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.feature;

import java.util.Set;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract test class for asserting a state of a particular feature after enabling/disabling
 */
public abstract class AbstractFeatureStateTest extends AbstractTestRealmKeycloakTest {

    public abstract String getFeatureProviderId();

    public abstract String getFeatureSpiName();

    @Test
    public void featureEnabled() {
        testFeatureAvailability(true);
    }

    @Test
    public void featureDisabled() {
        testFeatureAvailability(false);
    }

    public void testFeatureAvailability(boolean expectedAvailability) {
        ServerInfoRepresentation serverInfo = adminClient.serverInfo().getInfo();
        Set<String> authenticatorProviderIds = serverInfo.getProviders().get(getFeatureSpiName()).getProviders().keySet();
        Assert.assertEquals(expectedAvailability, authenticatorProviderIds.contains(getFeatureProviderId()));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
