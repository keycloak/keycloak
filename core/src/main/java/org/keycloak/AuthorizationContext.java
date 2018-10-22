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
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationContext {

    private final AccessToken authzToken;
    private final PathConfig current;
    private boolean granted;

    public AuthorizationContext(AccessToken authzToken, PathConfig current) {
        this.authzToken = authzToken;
        this.current = current;
        this.granted = true;
    }

    public AuthorizationContext() {
        this(null, null);
        this.granted = false;
    }

    public boolean hasPermission(String resourceName, String scopeName) {
        if (this.authzToken == null) {
            return false;
        }

        Authorization authorization = this.authzToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        for (Permission permission : authorization.getPermissions()) {
            if (resourceName.equalsIgnoreCase(permission.getResourceName()) || resourceName.equalsIgnoreCase(permission.getResourceId())) {
                if (scopeName == null) {
                    return true;
                }

                if (permission.getScopes().contains(scopeName)) {
                    return true;
                }
            }
        }

        if (current != null) {
            if (current.getName().equals(resourceName)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasResourcePermission(String resourceName) {
        return hasPermission(resourceName, null);
    }

    public boolean hasScopePermission(String scopeName) {
        if (this.authzToken == null) {
            return false;
        }

        Authorization authorization = this.authzToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        for (Permission permission : authorization.getPermissions()) {
            if (permission.getScopes().contains(scopeName)) {
                return true;
            }
        }

        return false;
    }

    public List<Permission> getPermissions() {
        if (this.authzToken == null) {
            return Collections.emptyList();
        }

        Authorization authorization = this.authzToken.getAuthorization();

        if (authorization == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(authorization.getPermissions()));
    }

    public boolean isGranted() {
        return granted;
    }
}
