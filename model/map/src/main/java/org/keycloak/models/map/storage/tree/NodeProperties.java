/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

/**
 *
 * @author hmlnarik
 */
public final class NodeProperties {

    /**
     * Defines the filter that must be satisfied for every entity within this store.
     * Type: {@link DefaultModelCriteria}
     */
    public static final String ENTITY_RESTRICTION = "entity-restriction";
    public static final String AUTHORITATIVE_DECIDER = "authoritative-decider";
    public static final String STRONGLY_AUTHORITATIVE = "strongly-authoritative";
    public static final String READ_ONLY = "read-only";
    public static final String REVALIDATE = "revalidate";

    public static final String AUTHORITATIVE_NODES = "___authoritative-nodes___";
    public static final String STORAGE_PROVIDER = "___storage-provider___";
    public static final String STORAGE_SUPPLIER = "___storage-supplier___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is primary source for the values of all attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for the values of attributes "address" and "logo".
     * </ul>
     */
    public static final String PRIMARY_SOURCE_FOR = "___primary-source-for___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is not primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is not primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is not primary source for the values of any attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for attributes apart from "address" and "logo" attributes.
     * </ul>
     */
    public static final String PRIMARY_SOURCE_FOR_EXCLUDED = "___primary-source-for-excluded___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is primary source for the values of all attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for the values of attributes "address" and "logo".
     * </ul>
     */
    public static final String CACHE_FOR = "___cache-for___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is not primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is not primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is not primary source for the values of any attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for attributes apart from "address" and "logo" attributes.
     * </ul>
     */
    public static final String CACHE_FOR_EXCLUDED = "___cache-for-excluded___";

}
