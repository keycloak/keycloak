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

package org.keycloak.adapters.saml.elytron;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.DefaultSamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.saml.common.exceptions.ParsingException;

/**
 * <p>A {@link ServletContextListener} that parses the keycloak adapter configuration and set the same configuration
 * as a {@link ServletContext} attribute in order to provide to {@link KeycloakHttpServerAuthenticationMechanism} a way
 * to obtain the configuration when processing requests.
 *
 * <p>This listener should be automatically registered to a deployment using the subsystem.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakConfigurationServletListener implements ServletContextListener {

    protected static Logger log = Logger.getLogger(KeycloakConfigurationServletListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        String configResolverClass = servletContext.getInitParameter("keycloak.config.resolver");
        SamlDeploymentContext deploymentContext = null;
        if (configResolverClass != null) {
            try {
                throw new RuntimeException("Not implemented yet");
                //configResolver = (SamlConfigResolver) deploymentInfo.getClassLoader().loadClass(configResolverClass).newInstance();
                //deploymentContext = new AdapterDeploymentContext(configResolver);
                //log.info("Using " + configResolverClass + " to resolve Keycloak configuration on a per-request basis.");
            } catch (Exception ex) {
                log.warn("The specified resolver " + configResolverClass + " could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: " + ex.getMessage());
                //deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream is = getConfigInputStream(servletContext);
            final SamlDeployment deployment;
            if (is == null) {
                log.warn("No adapter configuration.  Keycloak is unconfigured and will deny all requests.");
                deployment = new DefaultSamlDeployment();
            } else {
                try {
                    ResourceLoader loader = new ResourceLoader() {
                        @Override
                        public InputStream getResourceAsStream(String resource) {
                            return servletContext.getResourceAsStream(resource);
                        }
                    };
                    deployment = new DeploymentBuilder().build(is, loader);
                } catch (ParsingException e) {
                    throw new RuntimeException(e);
                }
            }
            deploymentContext = new SamlDeploymentContext(deployment);
            servletContext.setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);
            log.debug("Keycloak is using a per-deployment configuration.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    private static InputStream getConfigInputStream(ServletContext context) {
        InputStream is = getXMLFromServletContext(context);
        if (is == null) {
            String path = context.getInitParameter("keycloak.config.file");
            if (path == null) {
                log.debug("using /WEB-INF/keycloak-saml.xml");
                is = context.getResourceAsStream("/WEB-INF/keycloak-saml.xml");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }

    private static InputStream getXMLFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }
}
