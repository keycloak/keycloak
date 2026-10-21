/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.client.policies;

import java.util.Set;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionSpi;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorSpi;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.common.Profile.Feature.CLIENT_POLICIES;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test class is for enabling and disabling client policies by feature mechanism.
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPoliciesFeatureTest extends AbstractTestRealmKeycloakTest  {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testFeatureWorksWhenEnabled() {
        checkIfFeatureWorks(true);
    }

    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = CLIENT_POLICIES, skipRestart = true)
    public void testFeatureDoesntWorkWhenDisabled() {
        checkIfFeatureWorks(false);
    }

    // Check if the feature really works
    private void checkIfFeatureWorks(boolean shouldWork) {
        try {
            ClientPoliciesRepresentation clientPolicies = managedRealm.admin().clientPoliciesPoliciesResource().getPolicies();
            Assertions.assertTrue(clientPolicies.getPolicies().isEmpty());
            if (!shouldWork)
                fail("Feature is available, but at this moment should be disabled");

        } catch (Exception e) {
            if (shouldWork) {
                e.printStackTrace();
                fail("Feature is not available");
            }
        }

        ServerInfoRepresentation serverInfo = adminClient.serverInfo().getInfo();
        Set<String> executorProviderIds = serverInfo.getProviders().get(ClientPolicyExecutorSpi.SPI_NAME).getProviders().keySet();
        Set<String> conditionProviderIds = serverInfo.getProviders().get(ClientPolicyConditionSpi.SPI_NAME).getProviders().keySet();

        if (shouldWork) {
            Assertions.assertTrue(executorProviderIds.contains(SecureResponseTypeExecutorFactory.PROVIDER_ID));
            Assertions.assertTrue(conditionProviderIds.contains(ClientUpdaterContextConditionFactory.PROVIDER_ID));
        } else {
            Assertions.assertFalse(executorProviderIds.contains(SecureResponseTypeExecutorFactory.PROVIDER_ID));
            Assertions.assertFalse(conditionProviderIds.contains(ClientUpdaterContextConditionFactory.PROVIDER_ID));
        }
    }
}
