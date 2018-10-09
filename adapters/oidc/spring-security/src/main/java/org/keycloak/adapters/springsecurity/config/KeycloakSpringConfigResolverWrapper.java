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
package org.keycloak.adapters.springsecurity.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * Spring applications may use different security stacks in order to enforce access based on the configuration provided
 * by a {@code KeycloakDeployment}. This implementation of {@code KeycloakConfigResolver} wraps and avoid calling multiple
 * {@code KeycloakConfigResolver} instances but only those defined by applications or set as default by the configuration.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSpringConfigResolverWrapper implements KeycloakConfigResolver {

    private KeycloakConfigResolver delegate;

    public KeycloakSpringConfigResolverWrapper(KeycloakConfigResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public KeycloakDeployment resolve(HttpFacade.Request facade) {
        return delegate.resolve(facade);
    }

    protected void setDelegate(KeycloakConfigResolver delegate) {
        this.delegate = delegate;
    }
}
