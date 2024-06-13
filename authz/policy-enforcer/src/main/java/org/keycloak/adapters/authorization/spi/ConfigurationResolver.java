/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.authorization.spi;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

/**
 * Resolves a {@link PolicyEnforcerConfig} based on the information from the {@link HttpRequest}.</p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ConfigurationResolver {

    /**
     * Resolves a {@link PolicyEnforcerConfig} based on the information from the {@link HttpRequest}.
     *
     * @param request the request
     * @return the policy enforcer configuration for the given request
     */
    PolicyEnforcerConfig resolve(HttpRequest request);
}
