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
package org.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Use to unwrap exceptions specifically if there is an exception at JTA commit
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ExceptionConverter extends Provider, ProviderFactory<ExceptionConverter> {

    /**
     * Return null if the provider doesn't handle this type
     *
     * @param t
     * @return
     */
    Throwable convert(Throwable t);

    @Override
    default ExceptionConverter create(KeycloakSession session) {
        return this;
    }

    @Override
    default void init(Config.Scope config) {

    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    default void close() {

    }



}
