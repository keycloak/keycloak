/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.partialimport;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class PartialImportResult {

    private final Action action;
    private final String resourceType;
    private final String resourceName;
    private final Object representation;

    private PartialImportResult(Action action, ResourceType resourceType, String resourceName, Object representation) {
        this.action = action;
        this.resourceType = resourceType.toString();
        this.resourceName = resourceName;
        this.representation = representation;
    };

    public static PartialImportResult skipped(ResourceType resourceType, String resourceName, Object representation) {
        return new PartialImportResult(Action.SKIPPED, resourceType, resourceName, representation);
    }

    public static PartialImportResult added(ResourceType resourceType, String resourceName, Object representation) {
        return new PartialImportResult(Action.ADDED, resourceType, resourceName, representation);
    }

    public static PartialImportResult overwritten(ResourceType resourceType, String resourceName, Object representation) {
        return new PartialImportResult(Action.OVERWRITTEN, resourceType, resourceName, representation);
    }

    public Action getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    @JsonIgnore
    public Object getRepresentation() {
        return representation;
    }
}
