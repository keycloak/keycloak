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
package org.keycloak.services.clientregistration.policy.impl;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegistrationWebOriginsPolicyTest {

    private static KeycloakSession session;

    @BeforeClass
    public static void beforeClass() {
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
    }

    @Test
    public void testConstructionDoesNotMutateConfig() {
        // Issue #50286: the web-origins policy reads its config in the constructor;
        // that read must not mutate the shared cached ComponentModel.
        RegistrationWebOriginsPolicyFactory factory = new RegistrationWebOriginsPolicyFactory();
        ComponentModel model = new ComponentModel();

        assertFalse(model.getConfig().containsKey(RegistrationWebOriginsPolicyFactory.WEB_ORIGINS));
        RegistrationWebOriginsPolicy policy = (RegistrationWebOriginsPolicy) factory.create(session, model);
        assertFalse("constructing the policy must not mutate the shared component config",
                model.getConfig().containsKey(RegistrationWebOriginsPolicyFactory.WEB_ORIGINS));
        assertTrue("allowed origins must be empty when none are configured",
                policy.getAllowedOrigins().isEmpty());
    }
}
