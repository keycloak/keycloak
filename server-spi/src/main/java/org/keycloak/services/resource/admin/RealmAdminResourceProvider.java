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

package org.keycloak.services.resource.admin;

import org.keycloak.provider.Provider;

/**
 * <p>A {@link RealmAdminResourceProvider} creates JAX-RS <emphasis>sub-resource</emphasis> instances for paths relative to
 * Realm's Admin RESTful API that could not be resolved by the server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface RealmAdminResourceProvider extends Provider {

    /**
     * <p>Returns a JAX-RS resource instance that maps to the given <code>path</code>.
     *
     * <p>If the given <code>path</code> could not be resolved to a sub-resource, this method must return null to give a chance to other providers
     * to resolve their sub-resources.
     *
     * @param path the sub-resource's path
     * @return a JAX-RS sub-resource instance that maps to the given path or null if the path could not be resolved to a sub-resource.
     */
    Object getResource(String path);
}
