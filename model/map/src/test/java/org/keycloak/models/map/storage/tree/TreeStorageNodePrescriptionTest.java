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

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription.FieldContainedStatus;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

/**
 *
 * @author hmlnarik
 */
public class TreeStorageNodePrescriptionTest {

    @Test
    public void testEmpty() {
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(null);
        TreeStorageNodePrescription c1 = n.forEntityClass(MapClientEntity.class);
        TreeStorageNodePrescription c2 = n.forEntityClass(MapClientEntity.class);
        TreeStorageNodePrescription r1 = n.forEntityClass(MapRealmEntity.class);

        assertThat(c1, sameInstance(c2));
        assertThat(c1, not(sameInstance(r1)));
        assertThat(c1.getNodeProperties().entrySet(), empty());
        assertThat(c1.getEdgeProperties().entrySet(), empty());
        assertThat(c1.getTreeProperties().size(), is(1));

        assertThat(c1.getTreeProperties(), hasEntry(TreeProperties.MODEL_CLASS, ClientModel.class));
        assertThat(r1.getTreeProperties(), hasEntry(TreeProperties.MODEL_CLASS, RealmModel.class));
    }

    @Test
    public void testTreePropertyProjection() {
        Map<String, Object> treeProperties = new HashMap<>();
        treeProperties.put("prop[" + ModelEntityUtil.getModelName(ClientModel.class) + "]", "propClientValue");
        treeProperties.put("prop[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmValue");
        treeProperties.put("propRealmOnly[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmOnlyValue");
        treeProperties.put("propBoth", "propBothValue");

        Map<String, Object> nodeProperties = new HashMap<>();
        nodeProperties.put("nprop[" + ModelEntityUtil.getModelName(ClientModel.class) + "]", "propClientValue");
        nodeProperties.put("nprop[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmValue");
        nodeProperties.put("npropRealmOnly[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmOnlyValue");
        nodeProperties.put("npropBoth", "propBothValue");

        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("eprop[" + ModelEntityUtil.getModelName(ClientModel.class) + "]", "propClientValue");
        edgeProperties.put("eprop[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmValue");
        edgeProperties.put("epropRealmOnly[" + ModelEntityUtil.getModelName(RealmModel.class) + "]", "propRealmOnlyValue");
        edgeProperties.put("epropBoth", "propBothValue");

        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, edgeProperties, treeProperties);
        TreeStorageNodePrescription c1 = n.forEntityClass(MapClientEntity.class);
        TreeStorageNodePrescription r1 = n.forEntityClass(MapRealmEntity.class);

        assertThat(c1.getTreeProperties(), hasEntry(TreeProperties.MODEL_CLASS, ClientModel.class));
        assertThat(c1.getTreeProperties(), hasEntry("prop", "propClientValue"));
        assertThat(c1.getTreeProperties(), hasEntry("propBoth", "propBothValue"));
        assertThat(c1.getTreeProperties().size(), is(3));
        assertThat(c1.getNodeProperties(), hasEntry("nprop", "propClientValue"));
        assertThat(c1.getNodeProperties(), hasEntry("npropBoth", "propBothValue"));
        assertThat(c1.getNodeProperties().size(), is(2));
        assertThat(c1.getEdgeProperties(), hasEntry("eprop", "propClientValue"));
        assertThat(c1.getEdgeProperties(), hasEntry("epropBoth", "propBothValue"));
        assertThat(c1.getEdgeProperties().size(), is(2));

        assertThat(r1.getTreeProperties(), hasEntry(TreeProperties.MODEL_CLASS, RealmModel.class));
        assertThat(r1.getTreeProperties(), hasEntry("prop", "propRealmValue"));
        assertThat(r1.getTreeProperties(), hasEntry("propRealmOnly", "propRealmOnlyValue"));
        assertThat(r1.getTreeProperties(), hasEntry("propBoth", "propBothValue"));
        assertThat(r1.getTreeProperties().size(), is(4));
    }

    /**
     * Test a node that has neither PRIMARY_SOURCE_FOR nor PRIMARY_SOURCE_FOR_EXCLUDED set.
     * <p>
     * Represents e.g. a node in the tree that stores all fields.
     */
    @Test
    public void testPrimarySourceForBasicUnset() {
        Map<String, Object> nodeProperties = new HashMap<>();
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, null, null);

        for (MapClientEntityFields field : MapClientEntityFields.values()) {
            assertThat("Field " + field + " has primary source in this node", n.isPrimarySourceFor(field, null), is(FieldContainedStatus.FULLY));
        }
    }

    /**
     * Test a node that has PRIMARY_SOURCE_FOR set to all fields with no specialization (i.e. {@code null}),
     * and no PRIMARY_SOURCE_FOR_EXCLUDED is set.
     * <p>
     * Represents e.g. a node in the tree that stores all fields.
     */
    @Test
    public void testPrimarySourceForBasicSet() {
        Map<String, Object> nodeProperties = new HashMap<>();
        EnumMap<MapClientEntityFields, Collection<String>> primarySourceFor = new EnumMap<>(MapClientEntityFields.class);
        for (MapClientEntityFields field : MapClientEntityFields.values()) {
            primarySourceFor.put(field, null);
        }
        nodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR, primarySourceFor);
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, null, null);

        for (MapClientEntityFields field : MapClientEntityFields.values()) {
            assertThat("Field " + field + " has primary source in this node", n.isPrimarySourceFor(field, null), is(FieldContainedStatus.FULLY));
        }
    }

    /**
     * Test a node that has PRIMARY_SOURCE_FOR set to only ID field with no specialization (i.e. {@code null}),
     * and no PRIMARY_SOURCE_FOR_EXCLUDED is set.
     * <p>
     * Represents e.g. a node in the tree that stores only ID (maintains existence check of an object)
     */
    @Test
    public void testPrimarySourceForBasicSetId() {
        Map<String, Object> nodeProperties = new HashMap<>();
        EnumMap<MapClientEntityFields, Collection<String>> primarySourceFor = new EnumMap<>(MapClientEntityFields.class);
        nodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR, primarySourceFor);
        primarySourceFor.put(MapClientEntityFields.ID, null);
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, null, null);

        for (MapClientEntityFields field : MapClientEntityFields.values()) {
            assertThat(n.isPrimarySourceFor(field, null),
              is(field == MapClientEntityFields.ID ? FieldContainedStatus.FULLY : FieldContainedStatus.NOT_CONTAINED));
        }
    }

    /**
     * Test a node that has no PRIMARY_SOURCE_FOR set,
     * and PRIMARY_SOURCE_FOR_EXCLUDED is set to ATTRIBUTES field with no specialization (i.e. {@code null}).
     * <p>
     * Represents e.g. a node in the tree that stores all attributes apart from attributes
     */
    @Test
    public void testPrimarySourceForWithExcluded() {
        Map<String, Object> nodeProperties = new HashMap<>();
        EnumMap<MapClientEntityFields, Collection<String>> primarySourceForExcluded = new EnumMap<>(MapClientEntityFields.class);
        nodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, primarySourceForExcluded);
        primarySourceForExcluded.put(MapClientEntityFields.ATTRIBUTES, null);

        // node is primary for all fields apart from all attributes
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, null, null);

        for (MapClientEntityFields field : MapClientEntityFields.values()) {
            assertThat(n.isPrimarySourceFor(field, null),
              is(field == MapClientEntityFields.ATTRIBUTES ? FieldContainedStatus.NOT_CONTAINED: FieldContainedStatus.FULLY));
        }
    }

    /**
     * Test a node that has PRIMARY_SOURCE_FOR set to ATTRIBUTES field with no specialization (i.e. {@code null}),
     * and PRIMARY_SOURCE_FOR_EXCLUDED is set to ATTRIBUTES field specialization to "attr1" and "attr2".
     * <p>
     * Represents e.g. a node in the tree that acts as a supplementary store for all attributes apart from "attr1" and "attr2"
     */
    @Test
    public void testPrimarySourceForWithExcludedTwoAttributes() {
        Map<String, Object> nodeProperties = new HashMap<>();
        EnumMap<MapClientEntityFields, Collection<String>> primarySourceFor = new EnumMap<>(MapClientEntityFields.class);
        nodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR, primarySourceFor);
        primarySourceFor.put(MapClientEntityFields.ATTRIBUTES, null);
        EnumMap<MapClientEntityFields, Collection<String>> primarySourceForExcluded = new EnumMap<>(MapClientEntityFields.class);
        nodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, primarySourceForExcluded);
        primarySourceForExcluded.put(MapClientEntityFields.ATTRIBUTES, Arrays.asList("attr1", "attr2"));

        // node is primary for all attributes apart from "attr1" and "attr2"
        TreeStorageNodePrescription n = new TreeStorageNodePrescription(nodeProperties, null, null);

        assertThat("Field ID has NOT primary source in this node", n.isPrimarySourceFor(MapClientEntityFields.ID, null), is(FieldContainedStatus.NOT_CONTAINED));
        assertThat("Attribute attr1 has NOT primary source in this node", n.isPrimarySourceFor(MapClientEntityFields.ATTRIBUTES, "attr1"), is(FieldContainedStatus.NOT_CONTAINED));
        assertThat("Attribute attr2 has NOT primary source in this node", n.isPrimarySourceFor(MapClientEntityFields.ATTRIBUTES, "attr2"), is(FieldContainedStatus.NOT_CONTAINED));
        assertThat("Attribute attr3 has primary source in this node", n.isPrimarySourceFor(MapClientEntityFields.ATTRIBUTES, "attr3"), is(FieldContainedStatus.FULLY));
        assertThat("Attributes have primary source in this node and other source in some other nodes", n.isPrimarySourceFor(MapClientEntityFields.ATTRIBUTES, null), is(FieldContainedStatus.PARTIALLY));
    }

}
