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

package org.keycloak.adapters.springboot;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.WebResourceCollection;
import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.keycloak.adapters.undertow.KeycloakServletExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keycloak authentication integration for Spring Boot
 *
 * @author <a href="mailto:jimmidyson@gmail.com">Jimmi Dyson</a>
 * @version $Revision: 1 $
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(KeycloakSpringBootProperties.class)
public class KeycloakSpringBootConfiguration {

    private KeycloakSpringBootProperties keycloakProperties;

    @Autowired
    public void setKeycloakSpringBootProperties(KeycloakSpringBootProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
        KeycloakSpringBootConfigResolver.setAdapterConfig(keycloakProperties);
    }

    @Bean
    public EmbeddedServletContainerCustomizer getKeycloakContainerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {

                if (configurableEmbeddedServletContainer instanceof TomcatEmbeddedServletContainerFactory) {

                    TomcatEmbeddedServletContainerFactory container = (TomcatEmbeddedServletContainerFactory) configurableEmbeddedServletContainer;
                    container.addContextValves(new KeycloakAuthenticatorValve());
                    container.addContextCustomizers(tomcatKeycloakContextCustomizer());

                } else if (configurableEmbeddedServletContainer instanceof UndertowEmbeddedServletContainerFactory) {

                    UndertowEmbeddedServletContainerFactory container = (UndertowEmbeddedServletContainerFactory) configurableEmbeddedServletContainer;
                    container.addDeploymentInfoCustomizers(undertowKeycloakContextCustomizer());

                } else if (configurableEmbeddedServletContainer instanceof JettyEmbeddedServletContainerFactory) {

                    JettyEmbeddedServletContainerFactory container = (JettyEmbeddedServletContainerFactory) configurableEmbeddedServletContainer;
                    container.addServerCustomizers(jettyKeycloakServerCustomizer());
                }
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = {"org.eclipse.jetty.webapp.WebAppContext"})
    public JettyServerCustomizer jettyKeycloakServerCustomizer() {
        return new KeycloakJettyServerCustomizer(keycloakProperties);
    }

    @Bean
    @ConditionalOnClass(name = {"org.apache.catalina.startup.Tomcat"})
    public TomcatContextCustomizer tomcatKeycloakContextCustomizer() {
        return new KeycloakTomcatContextCustomizer(keycloakProperties);
    }

    @Bean
    @ConditionalOnClass(name = {"io.undertow.Undertow"})
    public UndertowDeploymentInfoCustomizer undertowKeycloakContextCustomizer() {
        return new KeycloakUndertowDeploymentInfoCustomizer(keycloakProperties);
    }

    static class KeycloakUndertowDeploymentInfoCustomizer implements UndertowDeploymentInfoCustomizer {

        private final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakUndertowDeploymentInfoCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        @Override
        public void customize(DeploymentInfo deploymentInfo) {

            io.undertow.servlet.api.LoginConfig loginConfig = new io.undertow.servlet.api.LoginConfig(keycloakProperties.getRealm());
            loginConfig.addFirstAuthMethod("KEYCLOAK");

            deploymentInfo.setLoginConfig(loginConfig);

            deploymentInfo.addInitParameter("keycloak.config.resolver", KeycloakSpringBootConfigResolver.class.getName());
            deploymentInfo.addSecurityConstraints(getSecurityConstraints());

            deploymentInfo.addServletExtension(new KeycloakServletExtension());
        }

        private List<io.undertow.servlet.api.SecurityConstraint> getSecurityConstraints() {

            List<io.undertow.servlet.api.SecurityConstraint> undertowSecurityConstraints = new ArrayList<io.undertow.servlet.api.SecurityConstraint>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraintDefinition : keycloakProperties.getSecurityConstraints()) {

                for (KeycloakSpringBootProperties.SecurityCollection collectionDefinition : constraintDefinition.getSecurityCollections()) {

                    io.undertow.servlet.api.SecurityConstraint undertowSecurityConstraint = new io.undertow.servlet.api.SecurityConstraint();
                    undertowSecurityConstraint.addRolesAllowed(collectionDefinition.getAuthRoles());

                    WebResourceCollection webResourceCollection = new WebResourceCollection();
                    webResourceCollection.addHttpMethods(collectionDefinition.getMethods());
                    webResourceCollection.addHttpMethodOmissions(collectionDefinition.getOmittedMethods());
                    webResourceCollection.addUrlPatterns(collectionDefinition.getPatterns());

                    undertowSecurityConstraint.addWebResourceCollections(webResourceCollection);

                    undertowSecurityConstraints.add(undertowSecurityConstraint);
                }
            }
            return undertowSecurityConstraints;
        }
    }

    static class KeycloakJettyServerCustomizer implements JettyServerCustomizer {

        private final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakJettyServerCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        @Override
        public void customize(Server server) {

            KeycloakJettyAuthenticator keycloakJettyAuthenticator = new KeycloakJettyAuthenticator();
            keycloakJettyAuthenticator.setConfigResolver(new KeycloakSpringBootConfigResolver());

            List<ConstraintMapping> jettyConstraintMappings = new ArrayList<ConstraintMapping>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraintDefinition : keycloakProperties.getSecurityConstraints()) {

                for (KeycloakSpringBootProperties.SecurityCollection securityCollectionDefinition : constraintDefinition
                        .getSecurityCollections()) {

                    Constraint jettyConstraint = new Constraint();
                    jettyConstraint.setName(securityCollectionDefinition.getName());
                    jettyConstraint.setAuthenticate(true);

                    if (securityCollectionDefinition.getName() != null) {
                        jettyConstraint.setName(securityCollectionDefinition.getName());
                    }

                    jettyConstraint.setRoles(securityCollectionDefinition.getAuthRoles().toArray(new String[0]));

                    ConstraintMapping jettyConstraintMapping = new ConstraintMapping();
                    if (securityCollectionDefinition.getPatterns().size() > 0) {
                        //First pattern wins
                        jettyConstraintMapping.setPathSpec(securityCollectionDefinition.getPatterns().get(0));
                        jettyConstraintMapping.setConstraint(jettyConstraint);
                    }

                    if (securityCollectionDefinition.getMethods().size() > 0) {
                        //First method wins
                        jettyConstraintMapping.setMethod(securityCollectionDefinition.getMethods().get(0));
                    }

                    jettyConstraintMapping.setMethodOmissions(
                            securityCollectionDefinition.getOmittedMethods().toArray(new String[0]));

                    jettyConstraintMappings.add(jettyConstraintMapping);
                }
            }

            WebAppContext webAppContext = server.getBean(WebAppContext.class);

            ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
            securityHandler.setConstraintMappings(jettyConstraintMappings);
            securityHandler.setAuthenticator(keycloakJettyAuthenticator);

            webAppContext.setHandler(securityHandler);
        }
    }

    static class KeycloakTomcatContextCustomizer implements TomcatContextCustomizer {

        private final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakTomcatContextCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        @Override
        public void customize(Context context) {
            LoginConfig loginConfig = new LoginConfig();
            loginConfig.setAuthMethod("KEYCLOAK");
            context.setLoginConfig(loginConfig);

            Set<String> authRoles = new HashSet<String>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraint : keycloakProperties.getSecurityConstraints()) {
                for (KeycloakSpringBootProperties.SecurityCollection collection : constraint.getSecurityCollections()) {
                    for (String authRole : collection.getAuthRoles()) {
                        if (!authRoles.contains(authRole)) {
                            context.addSecurityRole(authRole);
                            authRoles.add(authRole);
                        }
                    }
                }
            }

            for (KeycloakSpringBootProperties.SecurityConstraint constraint : keycloakProperties.getSecurityConstraints()) {
                SecurityConstraint tomcatConstraint = new SecurityConstraint();

                for (KeycloakSpringBootProperties.SecurityCollection collection : constraint.getSecurityCollections()) {
                    SecurityCollection tomcatSecCollection = new SecurityCollection();

                    if (collection.getName() != null) {
                        tomcatSecCollection.setName(collection.getName());
                    }
                    if (collection.getDescription() != null) {
                        tomcatSecCollection.setDescription(collection.getDescription());
                    }

                    for (String authRole : collection.getAuthRoles()) {
                        tomcatConstraint.addAuthRole(authRole);
                    }

                    for (String pattern : collection.getPatterns()) {
                        tomcatSecCollection.addPattern(pattern);
                    }

                    for (String method : collection.getMethods()) {
                        tomcatSecCollection.addMethod(method);
                    }

                    for (String method : collection.getOmittedMethods()) {
                        tomcatSecCollection.addOmittedMethod(method);
                    }

                    tomcatConstraint.addCollection(tomcatSecCollection);
                }

                context.addConstraint(tomcatConstraint);
            }

            context.addParameter("keycloak.config.resolver", KeycloakSpringBootConfigResolver.class.getName());
        }
    }
}
