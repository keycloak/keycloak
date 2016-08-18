/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.authorization.client.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>One or more resources that the resource server manages as a set of protected resources.
 *
 * <p>For more details, <a href="https://docs.kantarainitiative.org/uma/draft-oauth-resource-reg.html#rfc.section.2.2">OAuth-resource-reg</a>.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceRepresentation {

    @JsonProperty("_id")
    private String id;

    private String name;
    private String uri;
    private String type;
    private Set<ScopeRepresentation> scopes;

    @JsonProperty("icon_uri")
    private String iconUri;
    private String owner;

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param uri a {@link URI} that provides the network location for the resource set being registered
     * @param type a string uniquely identifying the semantics of the resource set
     * @param scopes the available scopes for this resource set
     * @param iconUri a {@link URI} for a graphic icon representing the resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, String uri, String type, String iconUri) {
        this.name = name;
        this.scopes = scopes;
        this.uri = uri;
        this.type = type;
        this.iconUri = iconUri;
    }

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param uri a {@link URI} that provides the network location for the resource set being registered
     * @param type a string uniquely identifying the semantics of the resource set
     * @param scopes the available scopes for this resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, String uri, String type) {
        this(name, scopes, uri, type, null);
    }

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param serverUri a {@link URI} that identifies this resource server
     * @param scopes the available scopes for this resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes) {
        this(name, scopes, null, null, null);
    }

    /**
     * Creates a new instance.
     *
     */
    public ResourceRepresentation() {
        this(null, null, null, null, null);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUri() {
        return this.uri;
    }

    public String getType() {
        return this.type;
    }

    public Set<ScopeRepresentation> getScopes() {
        return Collections.unmodifiableSet(this.scopes);
    }

    public String getIconUri() {
        return this.iconUri;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setScopes(Set<ScopeRepresentation> scopes) {
        this.scopes = scopes;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void addScope(ScopeRepresentation scopeRepresentation) {
        if (this.scopes == null) {
            this.scopes = new HashSet<>();
        }
        this.scopes.add(scopeRepresentation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceRepresentation that = (ResourceRepresentation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ResourceRepresentation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", type='" + type + '\'' +
                ", owner='" + owner + '\'' +
                ", scopes=" + scopes +
                '}';
    }
}
