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

package org.keycloak.protocol.oid4vc;

import org.keycloak.VCFormat;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.representations.idm.ClientScopeRepresentation;

import org.junit.After;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY;
import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_JWK;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT;

import static org.junit.Assert.assertEquals;

public class OID4VCLoginProtocolFactoryTest {

    private final OID4VCLoginProtocolFactory factory = new OID4VCLoginProtocolFactory();

    @After
    public void resetProfile() {
        Profile.reset();
    }

    @Test
    public void addClientScopeDefaultsKeepsMdocSuffixAsSdJwtWhenMdocFeatureDisabled() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(Feature.OID4VC_VCI.getVersionedKey(), ""));

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("natural-person_mdoc");

        factory.addClientScopeDefaults(clientScope);

        assertEquals(VCFormat.SD_JWT_VC, clientScope.getAttributes().get(VC_FORMAT));
        assertEquals(CRYPTOGRAPHIC_BINDING_METHOD_JWK, clientScope.getAttributes().get(VC_CRYPTOGRAPHIC_BINDING_METHODS));
    }

    @Test
    public void addClientScopeDefaultsInfersMdocSuffixWhenMdocFeatureEnabled() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(
                Feature.OID4VC_VCI.getVersionedKey() + "," + Feature.OID4VC_VCI_MDOC.getVersionedKey(), ""));

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("natural-person_mdoc");

        factory.addClientScopeDefaults(clientScope);

        assertEquals(VCFormat.MSO_MDOC, clientScope.getAttributes().get(VC_FORMAT));
        assertEquals(CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY, clientScope.getAttributes().get(VC_CRYPTOGRAPHIC_BINDING_METHODS));
    }

    @Test
    public void addClientScopeDefaultsKeepsJwtSuffixWhenMdocFeatureDisabled() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(Feature.OID4VC_VCI.getVersionedKey(), ""));

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("natural-person_jwt");

        factory.addClientScopeDefaults(clientScope);

        assertEquals(VCFormat.JWT_VC, clientScope.getAttributes().get(VC_FORMAT));
        assertEquals(CRYPTOGRAPHIC_BINDING_METHOD_JWK, clientScope.getAttributes().get(VC_CRYPTOGRAPHIC_BINDING_METHODS));
    }
}
