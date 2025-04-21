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

/**
 * Providers that are only supported in some environments can implement this interface to be able to determine if they
 * should be available or not.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EnvironmentDependentProviderFactory {

    /**
     * Check if the provider is supported and should be available based on the provider configuration.
     *
     * @param config the provider configuration
     * @return {@code true} if the provider is supported. Otherwise, {@code false}.
     */
    boolean isSupported(Config.Scope config);
}
