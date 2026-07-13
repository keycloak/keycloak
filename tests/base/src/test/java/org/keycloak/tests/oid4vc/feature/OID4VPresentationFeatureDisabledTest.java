/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oid4vc.feature;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCMinimalServerConfig.class)
public class OID4VPresentationFeatureDisabledTest extends OID4VCIssuerTestBase {

    @Test
    public void cannotCreateProviderWhenFeatureDisabled() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("oid4vp");
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(Map.of());

        try (Response response = testRealm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }
}
