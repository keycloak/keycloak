/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.model;

import org.keycloak.storage.SearchableModelField;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface PermissionTicket {

    public static class SearchableFields {
        public static final SearchableModelField<PermissionTicket> ID = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<PermissionTicket> RESOURCE_ID = new SearchableModelField<>("resourceId", String.class);
        public static final SearchableModelField<PermissionTicket> RESOURCE_SERVER_ID = new SearchableModelField<>("resourceServerId", String.class);
        public static final SearchableModelField<PermissionTicket> OWNER = new SearchableModelField<>("owner", String.class);
        public static final SearchableModelField<PermissionTicket> REQUESTER = new SearchableModelField<>("requester", String.class);
        public static final SearchableModelField<PermissionTicket> SCOPE_ID = new SearchableModelField<>("scopeId", String.class);
        public static final SearchableModelField<PermissionTicket> POLICY_ID = new SearchableModelField<>("policyId", String.class);
        public static final SearchableModelField<PermissionTicket> GRANTED_TIMESTAMP = new SearchableModelField<>("grantedTimestamp", String.class);
        public static final SearchableModelField<PermissionTicket> REALM_ID = new SearchableModelField<>("realmId", String.class);
    }
    
    public static enum FilterOption {
        ID("id", SearchableFields.ID),
        RESOURCE_ID("resource.id", SearchableFields.RESOURCE_ID),
        RESOURCE_NAME("resource.name", SearchableFields.RESOURCE_ID),
        SCOPE_ID("scope.id", SearchableFields.SCOPE_ID),
        SCOPE_IS_NULL("scope_is_null", SearchableFields.SCOPE_ID),
        OWNER("owner", SearchableFields.OWNER),
        GRANTED("granted", SearchableFields.GRANTED_TIMESTAMP),
        REQUESTER("requester", SearchableFields.REQUESTER),
        REQUESTER_IS_NULL("requester_is_null", SearchableFields.REQUESTER),
        POLICY_IS_NOT_NULL("policy_is_not_null", SearchableFields.POLICY_ID),
        POLICY_ID("policy.id", SearchableFields.POLICY_ID)
        ;

        private final String name;
        private final SearchableModelField<PermissionTicket> searchableModelField;

        FilterOption(String name, SearchableModelField<PermissionTicket> searchableModelField) {
            this.name = name;
            this.searchableModelField = searchableModelField;
        }


        public String getName() {
            return name;
        }

        public SearchableModelField<PermissionTicket> getSearchableModelField() {
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
     * Returns the resource's owner, which is usually an identifier that uniquely identifies the resource's owner.
     *
     * @return the owner of this resource
     */
    String getOwner();

    String getRequester();

    /**
     * Returns the {@link Resource} associated with this instance
     *
     * @return the {@link Resource} associated with this instance
     */
    Resource getResource();

    /**
     * Returns the {@link Scope} associated with this instance
     *
     * @return the {@link Scope} associated with this instance
     */
    Scope getScope();

    boolean isGranted();

    Long getCreatedTimestamp();

    Long getGrantedTimestamp();
    void setGrantedTimestamp(Long millis);

    /**
     * Returns the {@link ResourceServer} where this policy belongs to.
     *
     * @return a resource server
     */
    ResourceServer getResourceServer();

    Policy getPolicy();

    void setPolicy(Policy policy);
}
