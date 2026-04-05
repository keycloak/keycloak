/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;

import org.keycloak.representations.idm.authorization.ResourceType;

import static java.util.function.Predicate.not;

/**
 * An {@link PartialEvaluationContext} instance provides access to contextual information when building a query for realm
 * resources of a given {@link ResourceType}.
 */
public final class PartialEvaluationContext {

    private final ResourceType resourceType;
    private CriteriaQuery<?> criteriaQuery;
    private Path<?> path;
    private PartialEvaluationStorageProvider storage;
    private CriteriaBuilder criteriaBuilder;
    private final Set<String> allowedResources;
    private final Set<String> deniedResources;
    private Set<String> allowedGroups = Set.of();
    private Set<String> deniedGroups = Set.of();

    public PartialEvaluationContext(PartialEvaluationStorageProvider storage, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Path<?> path) {
        this(null, Set.of(), Set.of(), storage, criteriaBuilder, criteriaQuery, path);
    }

    public PartialEvaluationContext(ResourceType resourceType, Set<String> allowedResources, Set<String> deniedResources, PartialEvaluationStorageProvider storage, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Path<?> path) {
        this.allowedResources = allowedResources;
        this.deniedResources = deniedResources;
        this.storage = storage;
        this.criteriaBuilder = criteriaBuilder;
        this.criteriaQuery = criteriaQuery;
        this.path = path;
        this.resourceType = resourceType;
    }

    public boolean isResourceTypeAllowed() {
        return allowedResources.contains(resourceType.getType());
    }

    public Set<String> getAllowedResourceIds() {
        return allowedResources.stream().filter(not(resourceType.getType()::equals)).collect(Collectors.toSet());
    }

    public Set<String> getDeniedGroupIds() {
        return deniedGroups.stream().filter(not(resourceType.getGroupType()::equals)).collect(Collectors.toSet());
    }

    public void setAllowedGroups(Set<String> allowedGroups) {
        this.allowedGroups = allowedGroups;
    }

    public void setDeniedGroups(Set<String> deniedGroups) {
        this.deniedGroups = deniedGroups;
    }

    public Set<String> getDeniedGroups() {
        return deniedGroups;
    }

    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }

    public Set<String> getAllowedResources() {
        return allowedResources;
    }

    public Path<?> getPath() {
        return path;
    }

    public Set<String> deniedResources() {
        return deniedResources;
    }

    public CriteriaQuery<?> criteriaQuery() {
        return criteriaQuery;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public PartialEvaluationStorageProvider getStorage() {
        return storage;
    }

    public Set<String> getDeniedResources() {
        return deniedResources;
    }

    public void setStorage(PartialEvaluationStorageProvider storage) {
        this.storage = storage;
    }

    public void setCriteriaBuilder(CriteriaBuilder criteriaBuilder) {
        this.criteriaBuilder = criteriaBuilder;
    }

    public void setCriteriaQuery(CriteriaQuery<?> criteriaQuery) {
        this.criteriaQuery = criteriaQuery;
    }

    public void setPath(Path<?> path) {
        this.path = path;
    }
}
