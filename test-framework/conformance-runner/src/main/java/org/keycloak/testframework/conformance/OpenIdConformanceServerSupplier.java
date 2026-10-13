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

package org.keycloak.testframework.conformance;

import org.keycloak.testframework.conformance.annotations.InjectOpenIdConformanceServer;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;


public class OpenIdConformanceServerSupplier implements Supplier<OpenIdConformanceServer, InjectOpenIdConformanceServer> {

    @Override
    public OpenIdConformanceServer getValue(InstanceContext<OpenIdConformanceServer, InjectOpenIdConformanceServer> instanceContext) {
        return OpenIdConformanceServer.instance();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<OpenIdConformanceServer, InjectOpenIdConformanceServer> a,
            RequestedInstance<OpenIdConformanceServer, InjectOpenIdConformanceServer> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<OpenIdConformanceServer, InjectOpenIdConformanceServer> instanceContext) {
        instanceContext.getValue().close();
    }
}
