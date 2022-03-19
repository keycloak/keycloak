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
package org.keycloak.models.map.common.delegate;

import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.tree.NodeProperties;
import org.keycloak.models.map.storage.tree.TreeStorageNodeInstance;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author hmlnarik
 */
public class PerFieldDelegateProviderCacheTest {

    private MapClientEntity upperEnt;
    private MapClientEntity lowerEnt;

    private HashMap<String, Object> upperNodeProperties;
    private EnumMap<MapClientEntityFields, Object> upperCacheFor;
    private EnumMap<MapClientEntityFields, Object> upperCacheForExcluded;

    private HashMap<String, Object> lowerNodeProperties;
    private EnumMap<MapClientEntityFields, Object> lowerCacheFor;
    private EnumMap<MapClientEntityFields, Object> lowerCacheForExcluded;

    private TreeStorageNodeInstance<MapClientEntity> upperTsni;
    private TreeStorageNodeInstance<MapClientEntity> lowerTsni;

    AtomicInteger lowerEntSupplierCallCount = new AtomicInteger();

    @Before
    public void initEntities() {
        upperEnt = new MapClientEntityImpl();
        lowerEnt = new MapClientEntityImpl();

        upperEnt.setProtocol("upper-protocol");
        upperEnt.addRedirectUri("upper-redirectUri-1");
        upperEnt.addRedirectUri("upper-redirectUri-2");
        upperEnt.setClientId("upper-clientId-1");
        upperEnt.setAttribute("attr1", Arrays.asList("upper-value-1"));
        upperEnt.setAttribute("attr2", Arrays.asList("upper-value-2"));
        upperEnt.setAttribute("attr3", Arrays.asList("upper-value-3"));

        lowerEnt.setProtocol("lower-protocol");
        lowerEnt.addRedirectUri("lower-redirectUri-1");
        lowerEnt.addRedirectUri("lower-redirectUri-2");
        lowerEnt.setClientId("lower-clientId-1");
        lowerEnt.setAttribute("attr1", Arrays.asList("lower-value-1"));
        lowerEnt.setAttribute("attr3", Arrays.asList("lower-value-3"));
        lowerEnt.setAttribute("attr4", Arrays.asList("lower-value-4"));

        upperNodeProperties = new HashMap<>();
        upperCacheFor = new EnumMap<>(MapClientEntityFields.class);
        upperCacheForExcluded = new EnumMap<>(MapClientEntityFields.class);

        lowerNodeProperties = new HashMap<>();
        lowerCacheFor = new EnumMap<>(MapClientEntityFields.class);
        lowerCacheForExcluded = new EnumMap<>(MapClientEntityFields.class);

        lowerEntSupplierCallCount.set(0);
    }

    private MapClientEntity prepareEntityAndTreeNodeInstances() {
        TreeStorageNodePrescription upperTsnp = new TreeStorageNodePrescription(upperNodeProperties, null, null);
        TreeStorageNodePrescription lowerTsnp = new TreeStorageNodePrescription(lowerNodeProperties, null, null);

        upperTsni = new TreeStorageNodeInstance<>(null, upperTsnp);
        lowerTsni = new TreeStorageNodeInstance<>(null, lowerTsnp);

        PerFieldDelegateProvider<MapClientEntity> fieldProvider = new PerFieldDelegateProvider<>(upperTsni.new WithEntity(upperEnt), () -> {
            lowerEntSupplierCallCount.incrementAndGet();
            return lowerEnt;
        });
        return DeepCloner.DUMB_CLONER.entityFieldDelegate(MapClientEntity.class, fieldProvider);
    }

    @Test
    public void testGet_CacheFor() {
        //
        // High-level perspective: cache for listed fields, the primary source for values is in the lower entity.
        //
        // Upper node is not a primary source for any fields, and caches only the enumerated fields
        // Lower node is a primary source for all fields
        // There is an (intentional) discrepancy between the field values in lowerEnt and upperEnt to be able to distinguish
        // which entity the return value was obtained from.
        upperCacheFor.put(MapClientEntityFields.CLIENT_ID, null);
        upperCacheFor.put(MapClientEntityFields.ATTRIBUTES, Arrays.asList("attr2", "attr3", "attr4"));
        upperNodeProperties.put(NodeProperties.CACHE_FOR, upperCacheFor);
        upperNodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR, Collections.emptyMap());   // set the upper object to be exclusively a cache - none of the fields is primary source

        MapClientEntity ent = prepareEntityAndTreeNodeInstances();

        assertThat(lowerEntSupplierCallCount.get(), is(0));

        assertThat(ent.getClientId(), is("upper-clientId-1"));

        assertThat(ent.getAttribute("attr2"), contains("upper-value-2"));
        assertThat(ent.getAttribute("attr3"), contains("upper-value-3"));
        assertThat(ent.getAttribute("attr4"), nullValue());
        assertThat(lowerEntSupplierCallCount.get(), is(0));

        assertThat(ent.getProtocol(), is("lower-protocol"));
        assertThat(ent.getAttribute("attr1"), contains("lower-value-1"));
        assertThat(lowerEntSupplierCallCount.get(), is(1));

        assertThat(ent.getAttributes().keySet(), containsInAnyOrder("attr1", "attr2", "attr3"));
        assertThat(ent.getAttributes(), hasEntry("attr1", Arrays.asList("lower-value-1")));
        assertThat(ent.getAttributes(), hasEntry("attr2", Arrays.asList("upper-value-2")));
        assertThat(ent.getAttributes(), hasEntry("attr3", Arrays.asList("upper-value-3")));

        assertThat(lowerEntSupplierCallCount.get(), is(1));
    }

    @Test
    public void testSet_CacheFor() {
        //
        // High-level perspective: fields available in the lower layer (e.g. LDAP) cached in a faster layer (e.g. database)
        //
        // Upper node is a primary source for all fields apart from cached ones. It only caches only the enumerated fields
        // Lower node is a primary source for all fields
        // There is an (intentional) discrepancy between the field values in lowerEnt and upperEnt to be able to distinguish
        // which entity the return value was obtained from.
        upperCacheFor.put(MapClientEntityFields.CLIENT_ID, null);
        upperCacheFor.put(MapClientEntityFields.PROTOCOL, null);
        upperCacheFor.put(MapClientEntityFields.ATTRIBUTES, Arrays.asList("attr2", "attr3", "attr4"));
        upperNodeProperties.put(NodeProperties.CACHE_FOR, upperCacheFor);
        upperNodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, upperCacheFor);

        // When there is primary source in the node properties, named properties are considered as owned by this entity, and
        // all other are considered as owned by child entity.
        // Thus there is no call to the child entity creator.
        MapClientEntity ent = prepareEntityAndTreeNodeInstances();

        // When
        ent.setProtocol("modified-protocol");                           // modification in: upper and lower
        ent.setAttribute("attr1", Arrays.asList("modified-value-1"));   // modification in: upper
        ent.setAttribute("attrX", Arrays.asList("modified-value-X"));   // modification in: upper
        ent.addRedirectUri("added-redirectUri");                        // modification in: upper
        ent.removeRedirectUri("upper-redirectUri-2");                   // modification in: upper
        ent.removeRedirectUri("lower-redirectUri-2");                   // modification in: upper

        // Then
        assertThat(lowerEntSupplierCallCount.get(), is(1));

        assertThat(ent.getClientId(), is("upper-clientId-1"));
        assertThat(upperEnt.getClientId(), is("upper-clientId-1"));
        assertThat(lowerEnt.getClientId(), is("lower-clientId-1"));

        assertThat(ent.getRedirectUris(), containsInAnyOrder("upper-redirectUri-1", "added-redirectUri"));
        assertThat(upperEnt.getRedirectUris(), containsInAnyOrder("upper-redirectUri-1", "added-redirectUri"));
        assertThat(lowerEnt.getRedirectUris(), containsInAnyOrder("lower-redirectUri-1", "lower-redirectUri-2"));

        assertThat(ent.getProtocol(), is("modified-protocol"));
        assertThat(upperEnt.getProtocol(), is("modified-protocol"));
        assertThat(lowerEnt.getProtocol(), is("modified-protocol"));

        assertThat(ent.getAttribute("attr1"), contains("modified-value-1"));
        assertThat(upperEnt.getAttribute("attr1"), contains("modified-value-1"));
        assertThat(lowerEnt.getAttribute("attr1"), contains("lower-value-1"));

        assertThat(ent.getAttribute("attr3"), contains("upper-value-3"));
        assertThat(upperEnt.getAttribute("attr3"), contains("upper-value-3"));
        assertThat(lowerEnt.getAttribute("attr3"), contains("lower-value-3"));

        assertThat(ent.getAttribute("attrX"), contains("modified-value-X"));
        assertThat(upperEnt.getAttribute("attrX"), contains("modified-value-X"));
        assertThat(lowerEnt.getAttribute("attrX"), nullValue());

        assertThat(ent.getAttributes().keySet(), containsInAnyOrder(
          "attr1",  // From upper
          "attr2",  // From upper, since it is a "cached" value (deliberately a stale cache here, it is not in lower)
          "attr3",  // From upper
          // "attr4",  // From upper where it has no value (deliberately a stale cache here, it has a value in lower)
          "attrX"   // From upper
        ));
        assertThat(upperEnt.getAttributes().keySet(), containsInAnyOrder("attr1", "attr2", "attr3", "attrX"));
        assertThat(lowerEnt.getAttributes().keySet(), containsInAnyOrder("attr1", "attr3", "attr4"));
    }

    @Test
    public void testGet_CacheForExcluded() {
        //
        // High-level perspective: listed fields exclusively available in the lower layer (e.g. LDAP),
        //                         never stored nor cached in the top layer (e.g. database).
        //                         All other fields are stored in the top layer.
        //
        // Upper node is a primary source for all fields apart from cached ones. It only caches only the enumerated fields
        // Lower node is a primary source for all fields
        // There is an (intentional) discrepancy between the field values in lowerEnt and upperEnt to be able to distinguish
        // which entity the return value was obtained from.
        upperCacheForExcluded.put(MapClientEntityFields.CLIENT_ID, null);
        upperCacheForExcluded.put(MapClientEntityFields.ATTRIBUTES, Arrays.asList("attr2", "attr3", "attr4"));
        upperNodeProperties.put(NodeProperties.CACHE_FOR_EXCLUDED, upperCacheForExcluded);
        upperNodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, upperCacheForExcluded);

        MapClientEntity ent = prepareEntityAndTreeNodeInstances();

        assertThat(lowerEntSupplierCallCount.get(), is(0));
        assertThat(ent.getProtocol(), is("upper-protocol"));
        assertThat(ent.getAttribute("attr1"), contains("upper-value-1"));

        assertThat(lowerEntSupplierCallCount.get(), is(0));

        assertThat(ent.getAttribute("attr2"), nullValue());
        assertThat(lowerEntSupplierCallCount.get(), is(1));
        assertThat(ent.getAttribute("attr3"), contains("lower-value-3"));
        assertThat(ent.getAttribute("attr4"), contains("lower-value-4"));

        assertThat(ent.getClientId(), is("lower-clientId-1"));

        assertThat(ent.getAttributes().keySet(), containsInAnyOrder("attr1", "attr3", "attr4"));
        assertThat(ent.getAttributes(), hasEntry("attr1", Arrays.asList("upper-value-1")));
        assertThat(ent.getAttributes(), hasEntry("attr3", Arrays.asList("lower-value-3")));
        assertThat(ent.getAttributes(), hasEntry("attr4", Arrays.asList("lower-value-4")));

        assertThat(lowerEntSupplierCallCount.get(), is(1));
    }

    @Test
    public void testSet_CacheForExcluded() {
        //
        // High-level perspective: listed fields exclusively available in the lower layer (e.g. LDAP),
        //                         never stored nor cached in the top layer (e.g. database).
        //                         All other fields are stored in the top layer.
        //
        // When there is are primary source exclusion in the node properties, all properties apart from those enumerated
        // are considered as owned by this entity. Those enumerated are obtained from the child entity.
        // Thus there must be a single call to the child entity creator.
        upperCacheForExcluded.put(MapClientEntityFields.CLIENT_ID, null);
        upperCacheForExcluded.put(MapClientEntityFields.ATTRIBUTES, Arrays.asList("attr2", "attr3", "attr4"));
        upperNodeProperties.put(NodeProperties.CACHE_FOR_EXCLUDED, upperCacheForExcluded);
        upperNodeProperties.put(NodeProperties.PRIMARY_SOURCE_FOR_EXCLUDED, upperCacheForExcluded);

        // When there is primary source exclusion in the node properties, listed properties are considered as owned by the child
        // entity, and all other are considered as owned by this entity.
        // Thus there is no call to the child entity creator.
        MapClientEntity ent = prepareEntityAndTreeNodeInstances();

        // When
        ent.setClientId("modified-client-id-1");                        // modification in: lower
        ent.setProtocol("modified-protocol");                           // modification in: upper
        ent.setAttribute("attr4", Arrays.asList("modified-value-4"));   // modification in: lower
        ent.setAttribute("attrX", Arrays.asList("modified-value-X"));   // modification in: upper
        ent.addRedirectUri("added-redirectUri");                        // modification in: upper
        ent.removeRedirectUri("upper-redirectUri-2");                   // modification in: upper
        ent.removeRedirectUri("lower-redirectUri-2");                   // modification in: upper

        // Then
        assertThat(lowerEntSupplierCallCount.get(), is(1));

        assertThat(ent.getClientId(), is("modified-client-id-1"));
        assertThat(upperEnt.getClientId(), is("upper-clientId-1"));
        assertThat(lowerEnt.getClientId(), is("modified-client-id-1"));

        assertThat(ent.getRedirectUris(), containsInAnyOrder("upper-redirectUri-1", "added-redirectUri"));
        assertThat(upperEnt.getRedirectUris(), containsInAnyOrder("upper-redirectUri-1", "added-redirectUri"));
        assertThat(lowerEnt.getRedirectUris(), containsInAnyOrder("lower-redirectUri-1", "lower-redirectUri-2"));

        assertThat(ent.getProtocol(), is("modified-protocol"));
        assertThat(upperEnt.getProtocol(), is("modified-protocol"));
        assertThat(lowerEnt.getProtocol(), is("lower-protocol"));

        assertThat(ent.getAttribute("attr1"), contains("upper-value-1"));
        assertThat(upperEnt.getAttribute("attr1"), contains("upper-value-1"));
        assertThat(lowerEnt.getAttribute("attr1"), contains("lower-value-1"));

        assertThat(ent.getAttribute("attr2"), nullValue());
        assertThat(upperEnt.getAttribute("attr2"), contains("upper-value-2"));
        assertThat(lowerEnt.getAttribute("attr2"), nullValue());

        assertThat(ent.getAttribute("attr3"), contains("lower-value-3"));
        assertThat(upperEnt.getAttribute("attr3"), contains("upper-value-3"));
        assertThat(lowerEnt.getAttribute("attr3"), contains("lower-value-3"));

        assertThat(ent.getAttribute("attr4"), contains("modified-value-4"));
        assertThat(upperEnt.getAttribute("attr4"), nullValue());
        assertThat(lowerEnt.getAttribute("attr4"), contains("modified-value-4"));

        assertThat(ent.getAttribute("attrX"), contains("modified-value-X"));
        assertThat(upperEnt.getAttribute("attrX"), contains("modified-value-X"));
        assertThat(lowerEnt.getAttribute("attrX"), nullValue());

        assertThat(ent.getAttributes().keySet(), containsInAnyOrder(
          "attr1",  // From upper
          // "attr2",  // From lower where it has no value
          "attr3",  // From lower
          "attr4",  // From lower
          "attrX"   // From upper
        ));
        assertThat(upperEnt.getAttributes().keySet(), containsInAnyOrder("attr1", "attr2", "attr3", "attrX"));
        assertThat(lowerEnt.getAttributes().keySet(), containsInAnyOrder("attr1", "attr3", "attr4"));
    }

}
