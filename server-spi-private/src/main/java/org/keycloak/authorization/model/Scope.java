/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.model;

/**
 * Represents a scope, which is usually associated with one or more resources in order to define the actions that can be performed
 * or a specific access context.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Scope {

    public static enum FilterOption {
        ID("id"),
        NAME("name");

        private final String name;

        FilterOption(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Returns the unique identifier for this instance.
     *
     * @return the unique identifier for this instance
     */
    String getId();

    /**
     * Returns the name of this scope.
     *
     * @return the name of this scope
     */
    String getName();

    /**
     * Sets a name for this scope. The name must be unique.
     *
     * @param name the name of this scope
     */
    void setName(String name);

    /**
     * Returns the end user friendly name for this scope. If not defined, value for {@link #getName()} is returned.
     *
     * @return the friendly name for this scope
     */
    String getDisplayName();

    /**
     * Sets an end user friendly name for this scope.
     *
     * @param name the name of this scope
     */
    void setDisplayName(String name);

    /**
     * Returns an icon {@link java.net.URI} for this scope.
     *
     * @return a uri for an icon
     */
    String getIconUri();

    /**
     * Sets an icon {@link java.net.URI} for this scope.
     *
     * @return a uri for an icon
     */
    void setIconUri(String iconUri);

    /**
     * Returns the {@link ResourceServer} instance to where this scope belongs to.
     *
     * @return
     */
    ResourceServer getResourceServer();
}
