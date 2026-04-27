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
 */
package org.keycloak.testsuite.broker;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.representations.idm.ComponentExportRepresentation;

public abstract class AbstractKcOidcBrokerJWEEcdhEsTest extends KcOidcBrokerJWETest {

    private final String crv;
    private final String encAlg;

    public AbstractKcOidcBrokerJWEEcdhEsTest(String crv, String encAlg, String encEnc, String sigAlg) {
        super(encAlg, encEnc, sigAlg);
        this.crv = crv;
        this.encAlg = encAlg;
    }

    protected ComponentExportRepresentation getProviderKeyProvider() {
        // create the ECDSA component for the signature in the specified alg
        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setName("ecdsa-generated");
        component.setProviderId("ecdsa-generated");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("ecdsaEllipticCurveKey", this.crv);
        component.setConfig(config);

        return component;
    }

    protected ComponentExportRepresentation getConsumerKeyProvider() {
        // create the ECDH component for the encryption in the specified alg
        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setName("ecdh-generated");
        component.setProviderId("ecdh-generated");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("ecdhAlgorithm", this.encAlg);
        config.putSingle("ecdhEllipticCurveKey", this.crv);
        component.setConfig(config);

        return component;
    }
}
