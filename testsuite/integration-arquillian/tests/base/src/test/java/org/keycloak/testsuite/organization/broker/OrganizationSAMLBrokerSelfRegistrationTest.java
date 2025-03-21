/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.broker;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcSamlBrokerConfiguration;

public class OrganizationSAMLBrokerSelfRegistrationTest extends AbstractBrokerSelfRegistrationTest {

    @Override
    protected BrokerConfiguration createBrokerConfiguration() {
        return new KcSamlBrokerConfiguration() {

            @Override
            public String getIDPClientIdInProviderRealm() {
                return "saml-broker";
            }

            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation broker = super.setUpIdentityProvider(syncMode);
                broker.getConfig().put(SAMLIdentityProviderConfig.ENTITY_ID, getIDPClientIdInProviderRealm());
                return broker;
            }
        };
    }
}
