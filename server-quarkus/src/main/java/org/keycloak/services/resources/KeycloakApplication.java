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
package org.keycloak.services.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.util.JsonConfigProvider;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.servlet.ServletContext;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.DefaultKeycloakSessionFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ApplicationPath("/auth")
public class KeycloakApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(KeycloakApplication.class);

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory sessionFactory;
    protected String contextPath;

    public KeycloakApplication() {

        LOGGER.info("Keycloak.next starting up...");
        loadConfig();

        // FIXME: injecting @Context constructor params does not work in Quarkus
        ResteasyProviderFactory resteasy = ResteasyProviderFactory.getInstance();
        ServletContext context = resteasy.getContextData(ServletContext.class);
        Dispatcher dispatcher = resteasy.getContextData(Dispatcher.class);

        this.contextPath = context.getContextPath();
        this.sessionFactory = createSessionFactory();

        dispatcher.getDefaultContextObjects().put(KeycloakApplication.class, this);
        ResteasyContext.pushContext(KeycloakApplication.class, this); // for injection
        context.setAttribute(KeycloakSessionFactory.class.getName(), this.sessionFactory);

        singletons.add(new RobotsResource());
        singletons.add(new RealmsResource());
        singletons.add(new AdminRoot());
        classes.add(ThemeResource.class);
        classes.add(JsResource.class);

        LOGGER.infov("Master realm: {0}", Config.getAdminRealm());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public static void loadConfig() {
        try {
            JsonNode node = null;

            URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
            if (resource != null) {
                node = new ObjectMapper().readTree(resource);
            }

            if (node != null) {
                Map<String, String> propertyOverridesMap = new HashMap<>();
                Properties properties = new SystemEnvProperties(propertyOverridesMap);
                Config.init(new JsonConfigProvider(node, properties));
            } else {
                throw new RuntimeException("Keycloak config not found.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public static KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

}
