/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeSpi;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.feature.AbstractFeatureStateTest;

import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OID4VCGrantFeatureTest extends AbstractFeatureStateTest {

    @Override
    public String getFeatureProviderId() {
        return PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE;
    }

    @Override
    public String getFeatureSpiName() {
        return OAuth2GrantTypeSpi.SPI_NAME;
    }

    @Test
    @EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
    public void featureEnabled() {
        testFeatureAvailability(true);
    }

    @Test
    @DisableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
    public void featureDisabled() {
        testFeatureAvailability(false);
    }
}
