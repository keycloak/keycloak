/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

/**
 * Interface for tasks that compute a result and need access to the {@link KeycloakSession}.
 *
 * @param <V> the type of the computed result.
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@FunctionalInterface
public interface KeycloakSessionTaskWithResult<V> {

    /**
     * Computes a result.
     *
     * @param session a reference to the {@link KeycloakSession}.
     * @return the computed result.
     */
    V run(final KeycloakSession session);
}
