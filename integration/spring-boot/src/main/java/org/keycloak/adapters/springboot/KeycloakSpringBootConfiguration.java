package org.keycloak.adapters.springboot;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

/**
 * Keycloak authentication integration for Spring Boot
 *
 * @author <a href="mailto:jimmidyson@gmail.com">Jimmi Dyson</a>
 * @version $Revision: 1 $
 */
@Configuration
public class KeycloakSpringBootConfiguration {

    @Bean
    public EmbeddedServletContainerCustomizer getKeycloakContainerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
                if (configurableEmbeddedServletContainer instanceof TomcatEmbeddedServletContainerFactory) {
                    TomcatEmbeddedServletContainerFactory container = (TomcatEmbeddedServletContainerFactory) configurableEmbeddedServletContainer;

                    container.addContextValves(new KeycloakAuthenticatorValve());

                    container.addContextCustomizers(getKeycloakContextCustomizer());
                } else if (configurableEmbeddedServletContainer instanceof UndertowEmbeddedServletContainerFactory) {
                    throw new IllegalArgumentException("Undertow Keycloak integration is not yet implemented");
                } else if (configurableEmbeddedServletContainer instanceof JettyEmbeddedServletContainerFactory) {
                    throw new IllegalArgumentException("Jetty Keycloak integration is not yet implemented");
                }
            }
        };
    }

    @Bean
    public TomcatContextCustomizer getKeycloakContextCustomizer() {
        return new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                LoginConfig loginConfig = new LoginConfig();
                loginConfig.setAuthMethod("KEYCLOAK");
                context.setLoginConfig(loginConfig);

                context.addSecurityRole("jimmiapprole");

                SecurityConstraint constraint = new SecurityConstraint();
                constraint.addAuthRole("jimmiapprole");

                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);

                context.addConstraint(constraint);

                context.addParameter("keycloak.config.resolver", SpringBootKeycloakConfigResolver.class.getName());
            }
        };
    }

    public static class SpringBootKeycloakConfigResolver implements KeycloakConfigResolver {

        private KeycloakDeployment keycloakDeployment;

        @Override
        public KeycloakDeployment resolve(HttpFacade.Request request) {
            if (keycloakDeployment != null) {
                return keycloakDeployment;
            }

            InputStream configInputStream = getClass().getResourceAsStream("/keycloak.json");
            if (configInputStream == null) {
                keycloakDeployment = new KeycloakDeployment();
            } else {
                keycloakDeployment = KeycloakDeploymentBuilder.build(configInputStream);
            }

            return keycloakDeployment;
        }
    }


}
