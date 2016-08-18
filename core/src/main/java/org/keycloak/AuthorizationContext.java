/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationContext {

    private final AccessToken authzToken;
    private final List<PathConfig> paths;
    private boolean granted;

    public AuthorizationContext(AccessToken authzToken, List<PathConfig> paths) {
        this.authzToken = authzToken;
        this.paths = paths;
        this.granted = true;
    }

    public AuthorizationContext() {
        this(null, null);
        this.granted = false;
    }

    public boolean hasPermission(String resourceName, String scopeName) {
        for (Permission permission : authzToken.getAuthorization().getPermissions()) {
            for (PathConfig pathHolder : this.paths) {
                if (pathHolder.getName().equals(resourceName)) {
                    if (pathHolder.getId().equals(permission.getResourceSetId())) {
                        if (permission.getScopes().contains(scopeName)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean hasResourcePermission(String resourceName) {
        for (Permission permission : authzToken.getAuthorization().getPermissions()) {
            for (PathConfig pathHolder : this.paths) {
                if (pathHolder.getName().equals(resourceName)) {
                    if (pathHolder.getId().equals(permission.getResourceSetId())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasScopePermission(String scopeName) {
        for (Permission permission : authzToken.getAuthorization().getPermissions()) {
            if (permission.getScopes().contains(scopeName)) {
                return true;
            }
        }

        return false;
    }

    public List<Permission> getPermissions() {
        return this.authzToken.getAuthorization().getPermissions();
    }

    public boolean isGranted() {
        return granted;
    }
}
