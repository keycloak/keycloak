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

package org.keycloak.authorization.permission;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents a permission for a given resource.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermission {

    private final Resource resource;
    private final Collection<Scope> scopes;
    private ResourceServer resourceServer;
    private Map<String, Set<String>> claims;
    private boolean granted;

    public ResourcePermission(Resource resource, Collection<Scope> scopes, ResourceServer resourceServer) {
        this(resource, scopes, resourceServer, null);
    }

    public ResourcePermission(Resource resource, ResourceServer resourceServer, Map<String, ? extends Collection<String>> claims) {
        this(resource, new LinkedHashSet<>(), resourceServer, claims);
    }

    public ResourcePermission(Resource resource, Collection<Scope> scopes, ResourceServer resourceServer, Map<String, ? extends Collection<String>> claims) {
        this.resource = resource;
        this.scopes = scopes;
        this.resourceServer = resourceServer;
        if (claims != null) {
            this.claims = new HashMap<>();
            for (Entry<String, ? extends Collection<String>> entry : claims.entrySet()) {
                this.claims.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>()).addAll(entry.getValue());
            }
        }
    }

    /**
     * Returns the resource to which this permission applies.
     *
     * @return the resource to which this permission applies
     */
    public Resource getResource() {
        return this.resource;
    }

    /**
     * Returns a list of permitted scopes associated with the resource
     *
     * @return a lit of permitted scopes
     */
    public Collection<Scope> getScopes() {
        return this.scopes;
    }

    /**
     * Returns the resource server associated with this permission.
     *
     * @return the resource server
     */
    public ResourceServer getResourceServer() {
        return this.resourceServer;
    }

    /**
     * Returns all permission claims.
     *
     * @return
     */
    public Map<String, Set<String>> getClaims() {
        if (claims == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(claims);
    }

    /**
     * <p>Adds a permission claim with the given name and a single value.
     *
     * <p>If a claim already exists, the value is added to list of values of the existing claim</p>
     *
     * @param name the name of the claim
     * @param value the value of the claim
     */
    public boolean addClaim(String name, String value) {
        if (claims == null) {
            claims = new HashMap<>();
        }
        return claims.computeIfAbsent(name, key -> new HashSet<>()).add(value);
    }

    /**
     * <p>Removes a permission claim.
     *
     *
     * @param name the name of the claim
     */
    public void removeClaim(String name) {
        if (claims != null) {
            claims.remove(name);
        }
    }

    public void addScope(Scope scope) {
        if (resource != null) {
            if (!resource.getScopes().contains(scope)) {
                return;
            }
        }

        if (!scopes.contains(scope)) {
            scopes.add(scope);
        }
    }

    public void addClaims(Map<String, Set<String>> claims) {
        if (this.claims == null) {
            this.claims = new HashMap<>();
        }
        this.claims.putAll(claims);
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public boolean isGranted() {
        return granted;
    }
}
