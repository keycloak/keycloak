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

package org.keycloak.models.authorization.infinispan.entities;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedScope implements Scope, Serializable {

    private static final long serialVersionUID = -3919706923417065454L;

    private final String id;
    private String resourceServerId;
    private String name;
    private String iconUri;

    public CachedScope(Scope scope) {
        this.id = scope.getId();
        this.name = scope.getName();
        this.iconUri = scope.getIconUri();
        this.resourceServerId = scope.getResourceServer().getId();
    }

    public CachedScope(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getIconUri() {
        return this.iconUri;
    }

    @Override
    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    @Override
    public ResourceServer getResourceServer() {
        throw new RuntimeException("Not implemented");
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !Scope.class.isInstance(o)) return false;
        Scope that = (Scope) o;
        return Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
