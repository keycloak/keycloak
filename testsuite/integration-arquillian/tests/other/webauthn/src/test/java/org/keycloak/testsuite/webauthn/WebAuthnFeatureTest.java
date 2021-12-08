/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.authentication.AuthenticatorSpi;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

import java.util.Set;

@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public class WebAuthnFeatureTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testWebAuthnEnabled() {
        testWebAuthnAvailability(true);
    }

    @Test
    @DisableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true)
    public void testWebAuthnDisabled() {
        testWebAuthnAvailability(false);
    }

    private void testWebAuthnAvailability(boolean expectedAvailability) {
        ServerInfoRepresentation serverInfo = adminClient.serverInfo().getInfo();
        Set<String> authenticatorProviderIds = serverInfo.getProviders().get(AuthenticatorSpi.SPI_NAME).getProviders().keySet();
        Assert.assertEquals(expectedAvailability, authenticatorProviderIds.contains(WebAuthnAuthenticatorFactory.PROVIDER_ID));
    }
}
