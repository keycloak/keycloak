package org.keycloak.adapters.springboot;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
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

                    container.addContextCustomizers(getTomcatKeycloakContextCustomizer());
                } else if (configurableEmbeddedServletContainer instanceof UndertowEmbeddedServletContainerFactory) {
                    throw new IllegalArgumentException("Undertow Keycloak integration is not yet implemented");
                } else if (configurableEmbeddedServletContainer instanceof JettyEmbeddedServletContainerFactory) {
                    throw new IllegalArgumentException("Jetty Keycloak integration is not yet implemented");
                }
            }
        };
    }

    @Bean
    public TomcatContextCustomizer getTomcatKeycloakContextCustomizer() {
        return new TomcatContextCustomizer() {
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
        };
    }

}
