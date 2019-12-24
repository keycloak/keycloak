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
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.WebResourceCollection;
import org.apache.catalina.Context;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator;
import org.keycloak.adapters.undertow.KeycloakServletExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Keycloak authentication base integration for Spring Boot - base to be extended for particular boot versions.
 */
public class KeycloakBaseSpringBootConfiguration {
	
    protected KeycloakSpringBootProperties keycloakProperties;

    @Autowired
    public void setKeycloakSpringBootProperties(KeycloakSpringBootProperties keycloakProperties, KeycloakSpringBootConfigResolver resolver) {
        this.keycloakProperties = keycloakProperties;
        resolver.setAdapterConfig(keycloakProperties);
    }

    @Autowired
    public void setApplicationContext(ApplicationContext context) {
        KeycloakSpringBootConfigResolverWrapper.setApplicationContext(context);
    }

    static class KeycloakBaseUndertowDeploymentInfoCustomizer  {

        protected final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakBaseUndertowDeploymentInfoCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        public void customize(DeploymentInfo deploymentInfo) {

            io.undertow.servlet.api.LoginConfig loginConfig = new io.undertow.servlet.api.LoginConfig(keycloakProperties.getRealm());
            loginConfig.addFirstAuthMethod("KEYCLOAK");

            deploymentInfo.setLoginConfig(loginConfig);

            deploymentInfo.addInitParameter("keycloak.config.resolver", KeycloakSpringBootConfigResolverWrapper.class.getName());
            
            
            /* Support for '*' as all roles allowed
             * We clear out the role in the SecurityConstraints
             * and set the EmptyRoleSemantic to Authenticate
             * But we will set EmptyRoleSemantic to DENY (default)
             * if roles are non existing or left empty
             */
            Iterator<io.undertow.servlet.api.SecurityConstraint> it = this.getSecurityConstraints().iterator();
            while (it.hasNext()) {
            	io.undertow.servlet.api.SecurityConstraint securityConstraint = it.next();
            	Set<String> rolesAllowed = securityConstraint.getRolesAllowed();
            	
            	if (rolesAllowed.contains("*") || rolesAllowed.contains("**") ) {
            		io.undertow.servlet.api.SecurityConstraint allRolesAllowed = new io.undertow.servlet.api.SecurityConstraint();
            		allRolesAllowed.setEmptyRoleSemantic(EmptyRoleSemantic.AUTHENTICATE);
            		allRolesAllowed.setTransportGuaranteeType(securityConstraint.getTransportGuaranteeType());
            		for (WebResourceCollection wr : securityConstraint.getWebResourceCollections()) {
            			allRolesAllowed.addWebResourceCollection(wr);
            		}
            		deploymentInfo.addSecurityConstraint(allRolesAllowed);
            	} else // left empty will fall back on default EmptyRoleSemantic.DENY
            		deploymentInfo.addSecurityConstraint(securityConstraint);
            	
            }
            deploymentInfo.addServletExtension(new KeycloakServletExtension());
        }

        private List<io.undertow.servlet.api.SecurityConstraint> getSecurityConstraints() {

            List<io.undertow.servlet.api.SecurityConstraint> undertowSecurityConstraints = new ArrayList<io.undertow.servlet.api.SecurityConstraint>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraintDefinition : keycloakProperties.getSecurityConstraints()) {

                io.undertow.servlet.api.SecurityConstraint undertowSecurityConstraint = new io.undertow.servlet.api.SecurityConstraint();
                undertowSecurityConstraint.addRolesAllowed(constraintDefinition.getAuthRoles());

                for (KeycloakSpringBootProperties.SecurityCollection collectionDefinition : constraintDefinition.getSecurityCollections()) {

                    WebResourceCollection webResourceCollection = new WebResourceCollection();
                    webResourceCollection.addHttpMethods(collectionDefinition.getMethods());
                    webResourceCollection.addHttpMethodOmissions(collectionDefinition.getOmittedMethods());
                    webResourceCollection.addUrlPatterns(collectionDefinition.getPatterns());

                    undertowSecurityConstraint.addWebResourceCollections(webResourceCollection);

                }

                undertowSecurityConstraints.add(undertowSecurityConstraint);
            }
            return undertowSecurityConstraints;
        }
    }

    static class KeycloakBaseJettyServerCustomizer {

        protected final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakBaseJettyServerCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        public void customize(Server server) {

            KeycloakJettyAuthenticator keycloakJettyAuthenticator = new KeycloakJettyAuthenticator();
            keycloakJettyAuthenticator.setConfigResolver(new KeycloakSpringBootConfigResolverWrapper());

            /* see org.eclipse.jetty.webapp.StandardDescriptorProcessor#visitSecurityConstraint for an example
               on how to map servlet spec to Constraints */

            List<ConstraintMapping> jettyConstraintMappings = new ArrayList<ConstraintMapping>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraintDefinition : keycloakProperties.getSecurityConstraints()) {

                for (KeycloakSpringBootProperties.SecurityCollection securityCollectionDefinition : constraintDefinition
                        .getSecurityCollections()) {
                    // securityCollection matches servlet spec's web-resource-collection
                    Constraint jettyConstraint = new Constraint();

                    if (constraintDefinition.getAuthRoles().size() > 0) {
                        jettyConstraint.setAuthenticate(true);
                        jettyConstraint.setRoles(constraintDefinition.getAuthRoles().toArray(new String[0]));
                    }

                    jettyConstraint.setName(securityCollectionDefinition.getName());

                    // according to the servlet spec each security-constraint has at least one URL pattern
                    for(String pattern : securityCollectionDefinition.getPatterns()) {

                        /* the following code is asymmetric as Jetty's ConstraintMapping accepts only one allowed HTTP method,
                           but multiple omitted methods. Therefore we add one ConstraintMapping for each allowed
                           mapping but only one mapping in the cases of omitted methods or no methods.
                         */

                        if (securityCollectionDefinition.getMethods().size() > 0) {
                            // according to the servlet spec we have either methods ...
                            for(String method : securityCollectionDefinition.getMethods()) {
                                ConstraintMapping jettyConstraintMapping = new ConstraintMapping();
                                jettyConstraintMappings.add(jettyConstraintMapping);

                                jettyConstraintMapping.setConstraint(jettyConstraint);
                                jettyConstraintMapping.setPathSpec(pattern);
                                jettyConstraintMapping.setMethod(method);
                            }
                        } else if (securityCollectionDefinition.getOmittedMethods().size() > 0){
                            // ... omitted methods ...
                            ConstraintMapping jettyConstraintMapping = new ConstraintMapping();
                            jettyConstraintMappings.add(jettyConstraintMapping);

                            jettyConstraintMapping.setConstraint(jettyConstraint);
                            jettyConstraintMapping.setPathSpec(pattern);
                            jettyConstraintMapping.setMethodOmissions(
                                    securityCollectionDefinition.getOmittedMethods().toArray(new String[0]));
                        } else {
                            // ... or no methods at all
                            ConstraintMapping jettyConstraintMapping = new ConstraintMapping();
                            jettyConstraintMappings.add(jettyConstraintMapping);

                            jettyConstraintMapping.setConstraint(jettyConstraint);
                            jettyConstraintMapping.setPathSpec(pattern);
                        }

                    }

                }
            }

            WebAppContext webAppContext = server.getBean(WebAppContext.class);
            //if not found as registered bean let's try the handler
            if(webAppContext==null){
                webAppContext = getWebAppContext(server.getHandlers());
            }

            ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
            securityHandler.setConstraintMappings(jettyConstraintMappings);
            securityHandler.setAuthenticator(keycloakJettyAuthenticator);

            webAppContext.setSecurityHandler(securityHandler);
        }

        private WebAppContext getWebAppContext(Handler... handlers) {
            for (Handler handler : handlers) {
                if (handler instanceof WebAppContext) {
                    return (WebAppContext) handler;
                } else if (handler instanceof HandlerList) {
                    return getWebAppContext(((HandlerList) handler).getHandlers());
                } else if (handler instanceof HandlerCollection) {
                    return getWebAppContext(((HandlerCollection) handler).getHandlers());
                } else if (handler instanceof HandlerWrapper) {
                    return getWebAppContext(((HandlerWrapper) handler).getHandlers());
                }
            }
            throw new RuntimeException("No WebAppContext found in Jetty server handlers");
        }
    }

    static class KeycloakBaseTomcatContextCustomizer {

        protected final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakBaseTomcatContextCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        public void customize(Context context) {
            LoginConfig loginConfig = new LoginConfig();
            loginConfig.setAuthMethod("KEYCLOAK");
            context.setLoginConfig(loginConfig);

            Set<String> authRoles = new HashSet<String>();
            for (KeycloakSpringBootProperties.SecurityConstraint constraint : keycloakProperties.getSecurityConstraints()) {
                for (String authRole : constraint.getAuthRoles()) {
                    if (!authRoles.contains(authRole)) {
                        context.addSecurityRole(authRole);
                        authRoles.add(authRole);
                    }
                }
            }

            for (KeycloakSpringBootProperties.SecurityConstraint constraint : keycloakProperties.getSecurityConstraints()) {
                SecurityConstraint tomcatConstraint = new SecurityConstraint();
                for (String authRole : constraint.getAuthRoles()) {
                    tomcatConstraint.addAuthRole(authRole);
                    if(authRole.equals("*") || authRole.equals("**")) {
                        // For some reasons embed tomcat don't set the auth constraint on true when wildcard is used
                        tomcatConstraint.setAuthConstraint(true);
                    }
                }

                for (KeycloakSpringBootProperties.SecurityCollection collection : constraint.getSecurityCollections()) {
                    SecurityCollection tomcatSecCollection = new SecurityCollection();

                    if (collection.getName() != null) {
                        tomcatSecCollection.setName(collection.getName());
                    }
                    if (collection.getDescription() != null) {
                        tomcatSecCollection.setDescription(collection.getDescription());
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

            context.addParameter("keycloak.config.resolver", KeycloakSpringBootConfigResolverWrapper.class.getName());
        }
    }
}
