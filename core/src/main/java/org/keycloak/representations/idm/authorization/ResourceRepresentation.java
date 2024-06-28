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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.keycloak.json.StringListMapDeserializer;

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

    @JsonProperty("uris")
    private Set<String> uris;
    private String type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("scopes")
    private Set<ScopeRepresentation> scopes;

    @JsonProperty("icon_uri")
    private String iconUri;
    private ResourceOwnerRepresentation owner;
    private Boolean ownerManagedAccess;

    private String displayName;

    @JsonDeserialize(using = StringListMapDeserializer.class)
    private Map<String, List<String>> attributes;

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param uris a {@link List} of {@link URI} that provides network locations for the resource set being registered
     * @param type a string uniquely identifying the semantics of the resource set
     * @param scopes the available scopes for this resource set
     * @param iconUri a {@link URI} for a graphic icon representing the resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, Set<String> uris, String type, String iconUri) {
        this.name = name;
        this.scopes = scopes;
        this.uris = uris;
        this.type = type;
        this.iconUri = iconUri;
    }

    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, String uri, String type, String iconUri) {
        this(name, scopes, Collections.singleton(uri), type, iconUri);
    }

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param uris a {@link List} of {@link URI} that provides the network location for the resource set being registered
     * @param type a string uniquely identifying the semantics of the resource set
     * @param scopes the available scopes for this resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, Set<String> uris, String type) {
        this(name, scopes, uris, type, null);
    }

    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes, String uri, String type) {
        this(name, scopes, Collections.singleton(uri), type, null);
    }

    /**
     * Creates a new instance.
     *
     * @param name a human-readable string describing a set of one or more resources
     * @param serverUri a {@link URI} that identifies this resource server
     * @param scopes the available scopes for this resource set
     */
    public ResourceRepresentation(String name, Set<ScopeRepresentation> scopes) {
        this(name, scopes, (Set<String>) null, null, null);
    }

    public ResourceRepresentation(String name, String... scopes) {
        this.name = name;
        this.scopes = new HashSet<>();
        for (String s : scopes) {
            ScopeRepresentation rep = new ScopeRepresentation(s);
            this.scopes.add(rep);
        }
    }

    /**
     * Creates a new instance.
     *
     */
    public ResourceRepresentation() {
        this(null, null, (Set<String>) null, null, null);
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

    public String getDisplayName() {
        return displayName;
    }

    @Deprecated
    @JsonIgnore
    public String getUri() {
        if (this.uris == null || this.uris.isEmpty()) {
            return null;
        }

        return this.uris.iterator().next();
    }

    public Set<String> getUris() {
        return this.uris;
    }

    public String getType() {
        return this.type;
    }

    public Set<ScopeRepresentation> getScopes() {
        if (this.scopes == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(this.scopes);
    }

    public String getIconUri() {
        return this.iconUri;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Deprecated
    @JsonSetter("uri")
    public void setUri(String uri) {
        if (uri != null && !"".equalsIgnoreCase(uri.trim())) {
            this.uris = Collections.singleton(uri);
        }
    }

    public void setUris(Set<String> uris) {
        if (uris != null) {
            Set<String> resultSet = new HashSet<>();
            for (String uri : uris) {
                if (uri != null && !"".equalsIgnoreCase(uri.trim())) {
                    resultSet.add(uri);
                }
            }

            this.uris = resultSet;
        }
    }

    public void setType(String type) {
        if (type != null && !"".equalsIgnoreCase(type.trim())) {
            this.type = type;
        }
    }

    public void setScopes(Set<ScopeRepresentation> scopes) {
        this.scopes = scopes;
    }

    /**
     * TODO: This is a workaround to allow deserialization of UMA resource representation. Jackson 2.19+ support aliases, once we upgrade, change this.
     *
     * @param scopes
     */
    @JsonSetter("resource_scopes")
    private void setScopesUma(Set<ScopeRepresentation> scopes) {
        this.scopes = scopes;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public ResourceOwnerRepresentation getOwner() {
        return this.owner;
    }

    @JsonProperty
    public void setOwner(ResourceOwnerRepresentation owner) {
        this.owner = owner;
    }

    @JsonIgnore
    public void setOwner(String ownerId) {
        if (ownerId == null) {
            owner = null;
            return;
        }

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
        }

        owner.setId(ownerId);
    }

    public Boolean getOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public void setOwnerManagedAccess(Boolean ownerManagedAccess) {
        this.ownerManagedAccess = ownerManagedAccess;
    }

    public void addScope(String... scopeNames) {
        if (scopes == null) {
            scopes = new HashSet<>();
        }
        for (String scopeName : scopeNames) {
            scopes.add(new ScopeRepresentation(scopeName));
        }
    }

    public void addScope(ScopeRepresentation scope) {
        if (scopes == null) {
            scopes = new HashSet<>();
        }
        scopes.add(scope);
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceRepresentation scope = (ResourceRepresentation) o;
        return Objects.equals(getName(), scope.getName());
    }

    public int hashCode() {
        return Objects.hash(getName());
    }
}
