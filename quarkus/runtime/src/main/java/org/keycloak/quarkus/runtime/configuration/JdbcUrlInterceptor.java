/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * Interceptor for JDBC URL configuration to handle socket timeout parameters.
 */
public class JdbcUrlInterceptor implements ConfigSourceInterceptor {

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue value = context.proceed(name);
        
        if (value != null && name.contains("jdbc") && name.contains("url")) {
            String url = value.getValue();
            // Extract socketTimeout from URL if present
            if (url != null && url.contains("socketTimeout")) {
                // Store the timeout value for use by the connection interceptor
                String timeout = extractSocketTimeout(url);
                System.setProperty("kc.db.socket.timeout", timeout);
            }
        }
        
        return value;
    }

    private String extractSocketTimeout(String url) {
        // Extract socketTimeout parameter from JDBC URL
        int start = url.indexOf("socketTimeout=");
        if (start == -1) return null;
        
        start += "socketTimeout=".length();
        int end = url.indexOf("&", start);
        if (end == -1) end = url.length();
        
        return url.substring(start, end);
    }
}
