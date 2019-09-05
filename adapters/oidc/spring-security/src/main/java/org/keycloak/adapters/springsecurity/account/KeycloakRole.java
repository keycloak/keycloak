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

package org.keycloak.adapters.springsecurity.account;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * Represents an authority granted to an {@link Authentication} by the Keycloak server.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakRole implements GrantedAuthority {

    private String role;

    /**
     * Creates a new granted authority from the given Keycloak role.
     *
     * @param role the name of this granted authority
     */
    public KeycloakRole(String role) {
        Assert.notNull(role, "role cannot be null");
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GrantedAuthority)) {
            return false;
        }

        GrantedAuthority that = (GrantedAuthority) o;

        if (!role.equals(that.getAuthority())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 3 * role.hashCode();
    }

    @Override
    public String toString() {
        return "KeycloakRole{" +
                "role='" + role + '\'' +
                '}';
    }

}
