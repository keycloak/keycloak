package org.keycloak.adapters.authorization.integration.elytron;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.ServiceLoader;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.JsonSerialization;

/**
 * A {@link ServletContextListener} to programmatically configure the {@link ServletContext} in order to
 * enable the policy enforcer.</p>
 *
 * By default, the policy enforcer configuration is loaded from a file at {@code WEB-INF/policy-enforcer.json}.</p>
 *
 * Applications can also dynamically resolve the configuration by implementing the {@link ConfigurationResolver} SPI. For that,
 * make sure to create a {@link META-INF/services/org.keycloak.adapters.authorization.spi.ConfigurationResolver} to register
 * the implementation.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@WebListener
public class PolicyEnforcerServletContextListener implements ServletContextListener {

    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        Iterator<ConfigurationResolver> configResolvers = ServiceLoader.load(ConfigurationResolver.class).iterator();
        ConfigurationResolver configResolver;

        if (configResolvers.hasNext()) {
            configResolver = configResolvers.next();

            if (configResolvers.hasNext()) {
                throw new IllegalStateException("Multiple " + ConfigurationResolver.class.getName() + " implementations found");
            }

            logger.debugf("Configuration resolver found from classpath: %s", configResolver);
        } else {
            String enforcerConfigLocation = "WEB-INF/policy-enforcer.json";
            InputStream config = servletContext.getResourceAsStream(enforcerConfigLocation);

            if (config == null) {
                logger.debugf("Could not find the policy enforcer configuration file: %s", enforcerConfigLocation);
                return;
            }

            try {
                configResolver = createDefaultConfigurationResolver(JsonSerialization.readValue(config, PolicyEnforcerConfig.class));
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse policy enforcer configuration: " + enforcerConfigLocation);
            }
        }

        logger.debug("Policy enforcement filter is enabled.");

        servletContext.addFilter("keycloak-policy-enforcer", new ElytronPolicyEnforcerFilter(configResolver))
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }

    private ConfigurationResolver createDefaultConfigurationResolver(PolicyEnforcerConfig enforcerConfig) {
        return new ConfigurationResolver() {
            @Override
            public PolicyEnforcerConfig resolve(HttpRequest request) {
                return enforcerConfig;
            }
        };
    }
}
