/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;

/**
 * Test impersonation scenarios with both token-exchange:V1 and standard-token-exchange:V2 enabled. Impersonation requests should be handled by V1 implementation
 *
 * TODO: Remove this test once  standard-token-exchange supported by default. It won't be needed as SubjectImpersonationTokenExchangeV1 will have TE-v2 enabled by default
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
public class SubjectImpersonationTokenExchangeV1WithStandardV2EnabledTest extends AbstractSubjectImpersonationTokenExchangeTest {

    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    public void checkFeatureDisabled() {
        super.checkFeatureDisabled();
    }
}
