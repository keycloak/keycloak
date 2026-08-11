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

package org.keycloak.tests.conformance.containers;

import java.net.URI;
import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;


public class OpenIdConformanceSuiteSupplier implements Supplier<OpenIdConformanceSuite, InjectConformanceSuite> {

    @Override
    public OpenIdConformanceSuite getValue(InstanceContext<OpenIdConformanceSuite, InjectConformanceSuite> instanceContext) {
        // The KeycloakServer dependency ensures Keycloak is running before the suite containers connect to it
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        URI serverUri = URI.create(server.getBaseUrl());
        if (serverUri.getPort() != OpenIdConformanceSuite.KEYCLOAK_BASE_URI.getPort()) {
            throw new IllegalStateException("Keycloak server port " + serverUri.getPort()
                    + " does not match the port the conformance suite is configured to reach it at: "
                    + OpenIdConformanceSuite.KEYCLOAK_BASE_URI);
        }
        return OpenIdConformanceSuite.instance();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<OpenIdConformanceSuite, InjectConformanceSuite> a,
            RequestedInstance<OpenIdConformanceSuite, InjectConformanceSuite> b) {
        return true;
    }

    @Override
    public List<Dependency> getDependencies(RequestedInstance<OpenIdConformanceSuite, InjectConformanceSuite> instanceContext) {
        return DependenciesBuilder.create(KeycloakServer.class).build();
    }

    @Override
    public void close(InstanceContext<OpenIdConformanceSuite, InjectConformanceSuite> instanceContext) {
        instanceContext.getValue().close();
    }
}
