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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

/**
 * Prescription of the tree storage. This prescription can
 * be externalized and contains e.g. details on the particular storage type
 * represented by this node, or properties of the node and edge between this
 * and the parent storage.
 * <p>
 * Realization of this prescription is in {@link TreeStorageNodeInstance}, namely it does not
 * contain a map storage instance that can be directly used
 * for accessing data.
 *
 * @author hmlnarik
 */
public class TreeStorageNodePrescription extends DefaultTreeNode<TreeStorageNodePrescription> {

    private static final Logger LOG = Logger.getLogger(TreeStorageNodePrescription.class);
    private final boolean isPrimarySourceForAnything;
    private final boolean isPrimarySourceForEverything;
    private final boolean isCacheForAnything;

    public static enum FieldContainedStatus {
        /** 
         * Field is fully contained in the storage in this node.
         * For example, attribute {@code foo} or field {@code NAME} stored in this node would be {@code FULLY} contained field.
         * @see #PARTIALLY
         */
        FULLY {
            @Override
            public FieldContainedStatus minus(FieldContainedStatus s) {
                switch (s) {
                    case FULLY:
                        return FieldContainedStatus.NOT_CONTAINED;
                    case PARTIALLY:
                        return FieldContainedStatus.PARTIALLY;
                    default:
                        return FieldContainedStatus.FULLY;
                }
            }
        },
        /**
         * Field is contained in the storage in this node but parts of it might be contained in some child node as well.
         * For example, a few attributes can be partially supplied from an LDAP, and the rest could be supplied from the
         * supplementing JPA node.
         * <p>
         * This status is never used in the case of a fully specified field access but can be used for map-like attributes
         * where the key is not specified.
         */
        PARTIALLY {
            @Override
            public FieldContainedStatus minus(FieldContainedStatus s) {
                switch (s) {
                    case FULLY:
                        return FieldContainedStatus.NOT_CONTAINED;
                    default:
                        return FieldContainedStatus.PARTIALLY;
                }
            }
        },
        /** Field is not contained in the storage in this node but parts of it might be contained in some child node as well */
        NOT_CONTAINED {
            @Override
            public FieldContainedStatus minus(FieldContainedStatus s) {
                return FieldContainedStatus.NOT_CONTAINED;
            }
        };

        /**
         * Returns the status of the field if this field status in this node was stripped off the field status {@code s}.
         * Specifically, for two nodes in parent/child relationship, {@code parent.minus(child)} answers the question:
         * "How much of the field does parent contain that is not contained in the child?"
         * <ul>
         * <li>If the field in this node is contained {@link #FULLY} or {@link #PARTIALLY}, and the
         *     field in the other node is contained {@link #FULLY}, then
         *     there is no need to store any part of the field in this node,
         *     i.e. the result is {@link #NOT_CONTAINED}</li>
         * <li>If the field in this node is contained {@link #PARTIALLY} and the field in the other node is also only contained {@link #PARTIALLY}, then
         *     the portions might be disjunct, so it is still necessary to store a part of the field in this node, i.e. the result is {@link #PARTIALLY}</li>
         * <li>If the field in this node is {@link #NOT_CONTAINED} at all, then the result remains {@link #NOT_CONTAINED} regardless of the other node status</li>
         * </ul>
         */
        public abstract FieldContainedStatus minus(FieldContainedStatus s);
        /**
         * Returns higher of this and the {@code other} field status: {@link #FULLY} &gt; {@link #PARTIALLY} &gt; {@link #NOT_CONTAINED}
         */
        public FieldContainedStatus max(FieldContainedStatus other) {
            return (other == null || ordinal() < other.ordinal()) ? this : other;
        }
        public FieldContainedStatus max(Supplier<FieldContainedStatus> otherFunc) {
            if (ordinal() == 0) {
                return this;
            }
            FieldContainedStatus other = otherFunc.get();
            return other == null || ordinal() < other.ordinal() ? this : other;
        }
    }

    /**
     * Map of prescriptions restricted per entity class derived from this prescription.
     */
    private final Map<Class<? extends AbstractEntity>, TreeStorageNodePrescription> restricted = new ConcurrentHashMap<>();

    public TreeStorageNodePrescription(Map<String, Object> treeProperties) {
        this(treeProperties, null, null);
    }

    public TreeStorageNodePrescription(Map<String, Object> nodeProperties, Map<String, Object> edgeProperties, Map<String, Object> treeProperties) {
        super(nodeProperties, edgeProperties, treeProperties);
        Map<?, ?> psf = (Map<?, ?>) this.nodeProperties.get(NodeProperties.PRIMARY_SOURCE_FOR);
        Map<?, ?> psfe = (Map<?, ?>) this.nodeProperties.get(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED);
        isPrimarySourceForAnything = (psf != null && ! psf.isEmpty()) || (psfe != null && ! psfe.isEmpty());
        isPrimarySourceForEverything = (psf == null) && (psfe == null || psfe.isEmpty());   // This could be restricted further
        Map<?, ?> cf = (Map<?, ?>) this.nodeProperties.get(NodeProperties.CACHE_FOR);
        Map<?, ?> cfe = (Map<?, ?>) this.nodeProperties.get(NodeProperties.CACHE_FOR_EXCLUDED);
        isCacheForAnything = (cf != null && ! cf.isEmpty()) || (cfe != null && ! cfe.isEmpty());
    }

    public <V extends AbstractEntity> TreeStorageNodePrescription forEntityClass(Class<V> targetEntityClass) {
        return restricted.computeIfAbsent(targetEntityClass, c -> {
            HashMap<String, Object> treeProperties = new HashMap<>(restrictConfigMap(targetEntityClass, getTreeProperties()));
            treeProperties.put(TreeProperties.MODEL_CLASS, ModelEntityUtil.getModelType(targetEntityClass));
            return cloneTree(n -> n.forEntityClass(targetEntityClass, treeProperties));
        });
    }

    public <V extends AbstractEntity> TreeStorageNodeInstance<V> instantiate(KeycloakSession session) {
        return cloneTree(n -> new TreeStorageNodeInstance<>(session, n));
    }

    private <V extends AbstractEntity> TreeStorageNodePrescription forEntityClass(Class<V> targetEntityClass,
      Map<String, Object> treeProperties) {
        Map<String, Object> nodeProperties = restrictConfigMap(targetEntityClass, getNodeProperties());
        Map<String, Object> edgeProperties = restrictConfigMap(targetEntityClass, getEdgeProperties());

        if (nodeProperties == null || edgeProperties == null) {
            LOG.debugf("Cannot restrict storage for %s from node: %s", targetEntityClass, this);
            return null;
        }

        return new TreeStorageNodePrescription(nodeProperties, edgeProperties, treeProperties);
    }

    /**
     * Restricts configuration map to options that are either applicable everywhere (have no '[' in their name)
     * or apply to a selected entity class (e.g. ends in "[clients]" for {@code MapClientEntity}).
     * @return A new configuration map crafted for this particular entity class
     */
    private static <V extends AbstractEntity> Map<String, Object> restrictConfigMap(Class<V> targetEntityClass, Map<String, Object> np) {
        final Class<Object> modelType = ModelEntityUtil.getModelType(targetEntityClass, null);
        String name = Optional.ofNullable(modelType)
          .map(ModelEntityUtil::getModelName)
          .orElse(null);

        if (name == null) {
            return null;
        }

        // Start with all properties not specific for any domain
        Map<String, Object> res = np.entrySet().stream()
          .filter(me -> me.getKey().indexOf('[') == -1)
          .filter(me -> me.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (s, a) -> s, HashMap::new));

        // Now add and/or replace properties specific for the target domain
        Pattern specificPropertyPattern = Pattern.compile("(.*?)\\[.*\\b" + Pattern.quote(name) + "\\b.*\\]");
        np.keySet().stream()
          .map(specificPropertyPattern::matcher)
          .filter(Matcher::matches)
          .forEach(m -> res.put(m.group(1), np.get(m.group(0))));

        return res;
    }

    public boolean isReadOnly() {
        return getNodeProperty(NodeProperties.READ_ONLY, Boolean.class).orElse(false);
    }

    /**
     * Returns if the given field is primary source for the field, potentially specified further by a parameter.
     *
     * @param field Field
     * @param parameter First parameter, which in case of maps is the key to that map, e.g. attribute name.
     * @return For a fully specified field and a parameter (e.g. "attribute" and "attr1"), or a parameterless field (e.g. "client_id"),
     *   returns either {@code FULLY} or {@code NOT_CONTAINED}. May also return {@code PARTIAL} for a field that requires
     *   a parameter but the parameter is not specified.
     */
    public FieldContainedStatus isPrimarySourceFor(EntityField<?> field, Object parameter) {
        if (isPrimarySourceForEverything) {
            return FieldContainedStatus.FULLY;
        }
        if (! isPrimarySourceForAnything) {
            return FieldContainedStatus.NOT_CONTAINED;
        }

        FieldContainedStatus isPrimarySourceFor = getNodeProperty(NodeProperties.PRIMARY_SOURCE_FOR, Map.class)
          .map(m -> isFieldWithParameterIncludedInMap(m, field, parameter))
          .orElse(FieldContainedStatus.FULLY);

        FieldContainedStatus isExcludedPrimarySourceFor = getNodeProperty(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, Map.class)
          .map(m -> isFieldWithParameterIncludedInMap(m, field, parameter))
          .orElse(FieldContainedStatus.NOT_CONTAINED);

        return isPrimarySourceFor.minus(isExcludedPrimarySourceFor);
    }

    public FieldContainedStatus isCacheFor(EntityField<?> field, Object parameter) {
        if (! isCacheForAnything) {
            return FieldContainedStatus.NOT_CONTAINED;
        }

        // If there is CACHE_FOR_EXCLUDED then this node is a cache for all fields, and should be treated as such even if there is no CACHE_FOR
        // This is analogous to PRIMARY_SOURCE_FOR(_EXCLUDED).
        // However if there is neither CACHE_FOR_EXCLUDED nor CACHE_FOR, then this node is not a cache
        final Optional<Map> cacheForExcluded = getNodeProperty(NodeProperties.CACHE_FOR_EXCLUDED, Map.class);
        
        FieldContainedStatus isCacheFor = getNodeProperty(NodeProperties.CACHE_FOR, Map.class)
          .map(m -> isFieldWithParameterIncludedInMap(m, field, parameter))
          .orElse(cacheForExcluded.isPresent() ? FieldContainedStatus.FULLY : FieldContainedStatus.NOT_CONTAINED);

        FieldContainedStatus isExcludedCacheFor = cacheForExcluded
          .map(m -> isFieldWithParameterIncludedInMap(m, field, parameter))
          .orElse(FieldContainedStatus.NOT_CONTAINED);

        return isCacheFor.minus(isExcludedCacheFor);
    }

    private FieldContainedStatus isFieldWithParameterIncludedInMap(Map<?, ?> field2possibleParameters, EntityField<?> field, Object parameter) {
        Collection<?> specificCases = (Collection<?>) field2possibleParameters.get(field);
        if (specificCases == null) {
            return field2possibleParameters.containsKey(field)
              ? FieldContainedStatus.FULLY
              : FieldContainedStatus.NOT_CONTAINED;
        } else {
            return parameter == null
              ? FieldContainedStatus.PARTIALLY
              : specificCases.contains(parameter)
                ? FieldContainedStatus.FULLY
                : FieldContainedStatus.NOT_CONTAINED;
        }
    }

    @Override
    protected String getLabel() {
        return getId() + getNodeProperty(NodeProperties.STORAGE_PROVIDER, String.class).map(s -> " [" + s + "]").orElse("");
    }
}
