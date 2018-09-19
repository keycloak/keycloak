/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.constants.AdapterConstants;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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

    static final String ADAPTER_DEPLOYMENT_CONTEXT_ATTRIBUTE = AdapterDeploymentContext.class.getName();
    static final String ADAPTER_DEPLOYMENT_CONTEXT_ATTRIBUTE_ELYTRON = AdapterDeploymentContext.class.getName() + ".elytron";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        String configResolverClass = servletContext.getInitParameter("keycloak.config.resolver");
        KeycloakConfigResolver configResolver;
        AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext) servletContext.getAttribute(AdapterDeploymentContext.class.getName());

        if (deploymentContext == null) {
            if (configResolverClass != null) {
                try {
                    configResolver = (KeycloakConfigResolver) servletContext.getClassLoader().loadClass(configResolverClass).newInstance();
                    deploymentContext = new AdapterDeploymentContext(configResolver);
                } catch (Exception ex) {
                    deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
                }
            } else {
                InputStream is = getConfigInputStream(servletContext);

                KeycloakDeployment deployment;

                if (is == null) {
                    deployment = new KeycloakDeployment();
                } else {
                    deployment = KeycloakDeploymentBuilder.build(is);
                }

                deploymentContext = new AdapterDeploymentContext(deployment);
            }
        }

        servletContext.setAttribute(ADAPTER_DEPLOYMENT_CONTEXT_ATTRIBUTE, deploymentContext);
        servletContext.setAttribute(ADAPTER_DEPLOYMENT_CONTEXT_ATTRIBUTE_ELYTRON, deploymentContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    private InputStream getConfigInputStream(ServletContext servletContext) {
        InputStream is = getJSONFromServletContext(servletContext);

        if (is == null) {
            String path = servletContext.getInitParameter("keycloak.config.file");

            if (path == null) {
                is = servletContext.getResourceAsStream("/WEB-INF/keycloak.json");
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

    private InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);

        if (json == null) {
            return null;
        }

        return new ByteArrayInputStream(json.getBytes());
    }
}
