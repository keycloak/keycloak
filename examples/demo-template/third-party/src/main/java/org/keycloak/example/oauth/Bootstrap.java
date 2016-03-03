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

package org.keycloak.example.oauth;

import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.servlet.ServletOAuthClientBuilder;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Init code to load up the truststore so we can make appropriate SSL connections
 * You really should use a better way of initializing this stuff.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Bootstrap implements ServletContextListener {

    private ServletOAuthClient client;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        client = new ServletOAuthClient();
        ServletContext context = sce.getServletContext();

        configureClient(context);
        context.setAttribute(ServletOAuthClient.class.getName(), client);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        client.stop();
    }

    private void configureClient(ServletContext context) {
        InputStream is = null;
        String path = context.getInitParameter("keycloak.config.file");
        if (path == null) {
            is = context.getResourceAsStream("/WEB-INF/keycloak.json");
        } else {
            try {
                is = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        client = ServletOAuthClientBuilder.build(is);
    }
}
