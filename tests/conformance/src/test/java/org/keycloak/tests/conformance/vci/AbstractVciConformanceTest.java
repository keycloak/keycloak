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

package org.keycloak.tests.conformance.vci;

import org.keycloak.tests.conformance.AbstractConformanceTest;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for OID4VCI conformance tests. Test classes inject a realm with {@link VciConformanceRealmConfig} or
 * a subclass of it for additional configuration.
 */
abstract class AbstractVciConformanceTest extends AbstractConformanceTest {

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant module) {
        return VciSuiteConfig.create(
                OpenIdConformanceSuite.KEYCLOAK_BASE_URI,
                VciConformanceRealmConfig.attesterJwks(),
                VciTestSigningKey.caCertificatePem(),
                module.browserInteraction()).toJson();
    }
}
