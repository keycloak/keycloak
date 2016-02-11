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

package org.keycloak.services.resources.admin.spi;

import org.keycloak.provider.Provider;

/**
 * Provider for extending the admin REST API.
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 */
public interface RealmAdminResourceProvider extends Provider {

    /**
     * Get resource by path name. The provided path is a value that is not mapped
     * by the default admin resources. This provider should check if it does support
     * this path (match) and if there is a match, return a REST resource object.
     * If no match is found, it should return null.
     * 
     * @param pathName the path to be mapped
     * @return a REST resource (if path matches) or null (if path does not match)
     */
    Object getResource(String pathName);

}
