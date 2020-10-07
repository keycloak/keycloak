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

package org.keycloak.jaxrs;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.common.constants.GenericConstants;
import org.osgi.framework.BundleContext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Variant of JaxrsBearerTokenFilter, which can be used to properly use resources from current osgi bundle
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @deprecated Class is deprecated and may be removed in the future. If you want to maintain this class for Keycloak community, please
 * contact Keycloak team on keycloak-dev mailing list. You can fork it into your github repository and
 * Keycloak team will reference it from "Keycloak Extensions" page.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Deprecated
public class OsgiJaxrsBearerTokenFilterImpl extends JaxrsBearerTokenFilterImpl {

    private final static Logger log = Logger.getLogger("" + JaxrsBearerTokenFilterImpl.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        attemptStart();
    }

    @Override
    protected boolean isInitialized() {
        return super.isInitialized() && bundleContext != null;
    }

    @Override
    protected Class<? extends KeycloakConfigResolver> loadResolverClass() {
        String resolverClass = getKeycloakConfigResolverClass();
        try {
            return (Class<? extends KeycloakConfigResolver>) bundleContext.getBundle().loadClass(resolverClass);
        } catch (ClassNotFoundException cnfe) {
            log.warning("Not able to find class from bundleContext. Fallback to current classloader");
            return super.loadResolverClass();
        }
    }

    @Override
    protected InputStream loadKeycloakConfigFile() {
        String keycloakConfigFile = getKeycloakConfigFile();
        if (keycloakConfigFile.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {

            // Load from classpath of current bundle
            String classPathLocation = keycloakConfigFile.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
            log.fine("Loading config from classpath on location: " + classPathLocation);

            URL cfgUrl = bundleContext.getBundle().getResource(classPathLocation);
            if (cfgUrl == null) {
                log.warning("Not able to find configFile from bundleContext. Fallback to current classloader");
                return super.loadKeycloakConfigFile();
            }

            try {
                return cfgUrl.openStream();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            return super.loadKeycloakConfigFile();
        }
    }
}
