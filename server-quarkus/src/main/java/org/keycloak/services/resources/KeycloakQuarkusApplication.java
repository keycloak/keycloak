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
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.core.ResteasyContext;
import org.keycloak.Config;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.services.util.JsonConfigProvider;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.ApplicationPath;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ApplicationPath("")
public class KeycloakQuarkusApplication extends KeycloakApplication {

    // @Context injection doesn't work in the Application class yet
    // https://github.com/quarkusio/quarkus/issues/3535
    public KeycloakQuarkusApplication() {
        super(ResteasyContext.getContextData(ServletContext.class), ResteasyContext.getContextData(Dispatcher.class));
    }

    public static void loadConfig(ServletContext context) {
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

}
