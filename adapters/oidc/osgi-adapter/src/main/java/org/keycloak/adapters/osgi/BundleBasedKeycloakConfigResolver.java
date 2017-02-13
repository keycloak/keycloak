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

package org.keycloak.adapters.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BundleBasedKeycloakConfigResolver implements KeycloakConfigResolver {

    private volatile KeycloakDeployment cachedDeployment;

    private BundleContext bundleContext;
    private String configLocation = "WEB-INF/keycloak.json";

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    @Override
    public KeycloakDeployment resolve(HttpFacade.Request request) {
        if (cachedDeployment != null) {
            return cachedDeployment;
        } else {
            cachedDeployment = findDeployment(request);
            return cachedDeployment;
        }
    }

    protected KeycloakDeployment findDeployment(HttpFacade.Request request) {
        if (bundleContext == null) {
            throw new IllegalStateException("bundleContext must be set for BundleBasedKeycloakConfigResolver!");
        }

        URL url = bundleContext.getBundle().getResource(configLocation);
        if (url == null) {
            throw new IllegalStateException("Failed to find the file " + configLocation + " on classpath.");
        }

        try {
            InputStream is = url.openStream();
            return KeycloakDeploymentBuilder.build(is);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error reading file' " + configLocation + "' from bundle classpath.", ioe);
        }
    }
}
