/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.keycloak.proxy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class SecurityInfo<T extends SecurityInfo> implements Cloneable {

    /**
     * Equivalent to {@see ServletSecurity.EmptyRoleSemantic} but with an additional mode to require authentication but no role
     * check.
     */
    public enum EmptyRoleSemantic {

        /**
         * Permit access to the resource without requiring authentication or role membership.
         */
        PERMIT,

        /**
         * Deny access to the resource regardless of the authentication state.
         */
        DENY,

        /**
         * Mandate authentication but authorize access as no roles to check against.
         */
        AUTHENTICATE;

    }

    private volatile EmptyRoleSemantic emptyRoleSemantic = EmptyRoleSemantic.DENY;
    private final Set<String> rolesAllowed = new HashSet<String>();

    public EmptyRoleSemantic getEmptyRoleSemantic() {
        return emptyRoleSemantic;
    }

    public T setEmptyRoleSemantic(final EmptyRoleSemantic emptyRoleSemantic) {
        this.emptyRoleSemantic = emptyRoleSemantic;
        return (T)this;
    }

    public T addRoleAllowed(final String role) {
        this.rolesAllowed.add(role);
        return (T) this;
    }

    public T addRolesAllowed(final String ... roles) {
        this.rolesAllowed.addAll(Arrays.asList(roles));
        return (T) this;
    }
    public T addRolesAllowed(final Collection<String> roles) {
        this.rolesAllowed.addAll(roles);
        return (T) this;
    }
    public Set<String> getRolesAllowed() {
        return new HashSet<String>(rolesAllowed);
    }

    @Override
    public T clone() {
        final SecurityInfo info = createInstance();
        info.emptyRoleSemantic = emptyRoleSemantic;
        info.rolesAllowed.addAll(rolesAllowed);
        return (T) info;
    }

    protected T createInstance() {
        return (T) new SecurityInfo();
    }
}
