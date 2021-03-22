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

package org.keycloak.authorization.model;

import org.keycloak.storage.SearchableModelField;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a resource, which is usually protected by a set of policies within a resource server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Resource {

    public static class SearchableFields {
        public static final SearchableModelField<Resource> ID = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<Resource> NAME = new SearchableModelField<>("name", String.class);
        public static final SearchableModelField<Resource> RESOURCE_SERVER_ID = new SearchableModelField<>("resourceServerId", String.class);
        public static final SearchableModelField<Resource> OWNER = new SearchableModelField<>("owner", String.class);
        public static final SearchableModelField<Resource> TYPE = new SearchableModelField<>("type", String.class);

        public static final SearchableModelField<Resource> URI = new SearchableModelField<>("uris", String.class);
        public static final SearchableModelField<Resource> SCOPE_ID = new SearchableModelField<>("scope", String.class);
        public static final SearchableModelField<Resource> OWNER_MANAGED_ACCESS = new SearchableModelField<>("ownerManagedAccess", Boolean.class);
    }
    
    public static enum FilterOption {
        ID("id", SearchableFields.ID),
        NAME("name", SearchableFields.NAME),
        EXACT_NAME("name", SearchableFields.NAME),
        OWNER("owner", SearchableFields.OWNER),
        TYPE("type", SearchableFields.TYPE),
        URI("uri", SearchableFields.URI),
        URI_NOT_NULL("uri_not_null", SearchableFields.URI),
        OWNER_MANAGED_ACCESS("ownerManagedAccess", SearchableFields.OWNER_MANAGED_ACCESS),
        SCOPE_ID("scopes.id", SearchableFields.SCOPE_ID);

        private final String name;
        private final SearchableModelField<Resource> searchableModelField;

        FilterOption(String name, SearchableModelField<Resource> searchableModelField) {
            this.name = name;
            this.searchableModelField = searchableModelField;
        }


        public String getName() {
            return name;
        }

        public SearchableModelField<Resource> getSearchableModelField() {
            return searchableModelField;
        }
    }


    /**
     * Returns the unique identifier for this instance.
     *
     * @return the unique identifier for this instance
     */
    String getId();

    /**
     * Returns the resource's name.
     *
     * @return the name of this resource
     */
    String getName();

    /**
     * Sets a name for this resource. The name must be unique.
     *
     * @param name the name of this resource
     */
    void setName(String name);

    /**
     * Returns the end user friendly name for this resource. If not defined, value for {@link #getName()} is returned.
     *
     * @return the friendly name for this resource
     */
    String getDisplayName();

    /**
     * Sets an end user friendly name for this resource.
     *
     * @param name the name of this resource
     */
    void setDisplayName(String name);

    /**
     * Returns a {@link List} containing all {@link java.net.URI} that uniquely identify this resource.
     *
     * @return a {@link List} if {@link java.net.URI} for this resource or empty list if not defined.
     */
    Set<String> getUris();

    /**
     * Sets a list of {@link java.net.URI} that uniquely identify this resource.
     *
     * @param uri an {@link java.net.URI} for this resource
     */
    void updateUris(Set<String> uri);


    /**
     * Returns a string representing the type of this resource.
     *
     * @return the type of this resource or null if not defined
     */
    String getType();

    /**
     * Sets a string representing the type of this resource.
     *
     * @return the type of this resource or null if not defined
     */
    void setType(String type);

    /**
     * Returns a {@link List} containing all the {@link Scope} associated with this resource.
     *
     * @return a list with all scopes associated with this resource
     */
     List<Scope> getScopes();

    /**
     * Returns an icon {@link java.net.URI} for this resource.
     *
     * @return a uri for an icon
     */
    String getIconUri();

    /**
     * Sets an icon {@link java.net.URI} for this resource.
     *
     * @return a uri for an icon
     */
    void setIconUri(String iconUri);

    /**
     * Returns the {@link ResourceServer} to where this resource belongs to.
     *
     * @return the resource server associated with this resource
     */
     String getResourceServer();

    /**
     * Returns the resource's owner, which is usually an identifier that uniquely identifies the resource's owner.
     *
     * @return the owner of this resource
     */
    String getOwner();

    /**
     * Indicates if this resource can be managed by the resource owner.
     *
     * @return {@code true} if this resource can be managed by the resource owner. Otherwise, {@code false}.
     */
    boolean isOwnerManagedAccess();

    /**
     * Sets if this resource can be managed by the resource owner.
     *
     * @param ownerManagedAccess {@code true} indicates that this resource can be managed by the resource owner.
     */
    void setOwnerManagedAccess(boolean ownerManagedAccess);

    /**
     * Update the set of scopes associated with this resource.
     *
     * @param scopes the list of scopes to update
     */
    void updateScopes(Set<Scope> scopes);

    /**
     * Returns the attributes associated with this resource.
     *
     * @return a map holding the attributes associated with this resource
     */
    Map<String, List<String>> getAttributes();

    /**
     * Returns the first value of an attribute with the given <code>name</code>
     *
     * @return the first value of an attribute
     */
    String getSingleAttribute(String name);

    /**
     * Returns the values of an attribute with the given <code>name</code>
     *
     * @return the values of an attribute
     */
    List<String> getAttribute(String name);

    /**
     * Sets an attribute with the given <code>name</code> and <code>values</code>.
     *
     * @param name the attribute name
     * @param value the attribute values
     * @return a map holding the attributes associated with this resource
     */
    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);
}
