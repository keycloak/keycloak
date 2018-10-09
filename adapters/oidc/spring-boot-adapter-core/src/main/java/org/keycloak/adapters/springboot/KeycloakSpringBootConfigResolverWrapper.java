/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springsecurity.config.KeycloakSpringConfigResolverWrapper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * <p>A specific implementation of {@link KeycloakSpringConfigResolverWrapper} that first tries to register any {@link KeycloakConfigResolver}
 * instance provided by the application. if none is provided, {@link KeycloakSpringBootConfigResolver} is set.
 *
 * <p>This implementation is specially useful when using Spring Boot and Spring Security in the same application where the same {@link KeycloakConfigResolver}
 * instance must be used across the different stacks.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSpringBootConfigResolverWrapper extends KeycloakSpringConfigResolverWrapper {

    private static ApplicationContext context;

    public KeycloakSpringBootConfigResolverWrapper() {
        super(new KeycloakSpringBootConfigResolver());
        try {
            setDelegate(context.getBean(KeycloakConfigResolver.class));
        } catch (NoSuchBeanDefinitionException ignore) {
        }
    }

    public static void setApplicationContext(ApplicationContext context) {
        KeycloakSpringBootConfigResolverWrapper.context = context;
    }
}
