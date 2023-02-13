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

import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;




/**
 * Keycloak authentication integration for Spring Boot 2
 *
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(KeycloakSpringBootProperties.class)
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
public class KeycloakAutoConfiguration extends KeycloakBaseSpringBootConfiguration {


    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> getKeycloakContainerCustomizer() {
        return new WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>() {
            @Override
            public void customize(ConfigurableServletWebServerFactory configurableServletWebServerFactory) {
                if(configurableServletWebServerFactory instanceof TomcatServletWebServerFactory){

                    TomcatServletWebServerFactory container = (TomcatServletWebServerFactory)configurableServletWebServerFactory;
                    container.addContextValves(new KeycloakAuthenticatorValve());
                    container.addContextCustomizers(tomcatKeycloakContextCustomizer());

                } else if (configurableServletWebServerFactory instanceof UndertowServletWebServerFactory){

                    UndertowServletWebServerFactory container = (UndertowServletWebServerFactory)configurableServletWebServerFactory;
                    container.addDeploymentInfoCustomizers(undertowKeycloakContextCustomizer());

                } else if (configurableServletWebServerFactory instanceof JettyServletWebServerFactory){

                    JettyServletWebServerFactory container = (JettyServletWebServerFactory)configurableServletWebServerFactory;
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

    static class KeycloakJettyServerCustomizer extends KeycloakBaseJettyServerCustomizer implements JettyServerCustomizer {


        public KeycloakJettyServerCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            super(keycloakProperties);
        }

    }

    static class KeycloakTomcatContextCustomizer extends KeycloakBaseTomcatContextCustomizer implements TomcatContextCustomizer {

        public KeycloakTomcatContextCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            super(keycloakProperties);
        }
    }

    static class KeycloakUndertowDeploymentInfoCustomizer extends KeycloakBaseUndertowDeploymentInfoCustomizer implements UndertowDeploymentInfoCustomizer {

        public KeycloakUndertowDeploymentInfoCustomizer(KeycloakSpringBootProperties keycloakProperties){
            super(keycloakProperties);
        }
    }
}
