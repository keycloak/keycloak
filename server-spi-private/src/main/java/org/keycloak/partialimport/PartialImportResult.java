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

package org.keycloak.partialimport;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class represents a single result for a resource imported.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class PartialImportResult {

    private Action action;
    private ResourceType resourceType;
    private String resourceName;
    private String id;
    private Object representation;

    private PartialImportResult() {};

    private PartialImportResult(Action action, ResourceType resourceType, String resourceName, String id, Object representation) {
        this.action = action;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.id = id;
        this.representation = representation;
    };

    public static PartialImportResult skipped(ResourceType resourceType, String resourceName, String id, Object representation) {
        return new PartialImportResult(Action.SKIPPED, resourceType, resourceName, id, representation);
    }

    public static PartialImportResult added(ResourceType resourceType, String resourceName, String id, Object representation) {
        return new PartialImportResult(Action.ADDED, resourceType, resourceName, id, representation);
    }

    public static PartialImportResult overwritten(ResourceType resourceType, String resourceName, String id, Object representation) {
        return new PartialImportResult(Action.OVERWRITTEN, resourceType, resourceName, id, representation);
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public Object getRepresentation() {
        return representation;
    }
}
