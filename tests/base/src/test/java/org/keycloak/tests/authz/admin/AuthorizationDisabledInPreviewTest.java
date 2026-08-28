/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.authz.admin;

import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = AuthorizationDisabledInPreviewTest.AuthorizationDisabledServerConfig.class)
public class AuthorizationDisabledInPreviewTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    @UncaughtServerErrorExpected
    public void testAuthzServicesRemoved() {
        String id = managedRealm.admin().clients().findAll().get(0).getId();
        try {
            managedRealm.admin().clients().get(id).authorization().getSettings();
        } catch (ServerErrorException e) {
            assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), e.getResponse().getStatus());
            return;
        }
        fail("Feature Authorization should be disabled.");
    }

    public static class AuthorizationDisabledServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.AUTHORIZATION);
        }
    }

}
