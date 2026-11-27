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
package org.keycloak.representations.idm.authorization;

import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePermissionRepresentation extends AbstractPolicyRepresentation {

    private Map<String, String> config;

    @Override
    public String getType() {
        return "scope";
    }

    /**
     * Settings specific to the resource type of the permission. Only applicable to admin permissions, and only the keys
     * supported by the resource type are accepted. For instance, permissions for the {@code Groups} resource type
     * support {@code extendChildren} to also apply the permission to all the descendants of its groups.
     *
     * @return the settings of the permission, or {@code null} if not set, in which case they are not changed on update
     */
    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
