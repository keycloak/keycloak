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
package org.keycloak.models.map.client;

import org.keycloak.models.map.common.DeepCloner;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.keycloak.models.map.common.DeepCloner.DUMB_CLONER;

/**
 *
 * @author hmlnarik
 */
public class MapClientEntityClonerTest {

    private final static DeepCloner CLONER = new DeepCloner.Builder()
      .constructor(MapClientEntityImpl.class,           MapClientEntityImpl::new)
      .constructor(MapProtocolMapperEntity.class,       MapProtocolMapperEntityImpl::new)
      .build();

    @Test
    public void testNewInstance() {
        MapClientEntity newInstance = CLONER.newInstance(MapClientEntity.class);
        assertThat(newInstance, instanceOf(MapClientEntityImpl.class));
        assertThat(newInstance.getId(), nullValue());
    }

    @Test
    public void testNewInstanceWithId() {
        MapClientEntity newInstance = CLONER.newInstance(MapClientEntity.class);
        newInstance.setId("my-id");
        assertThat(newInstance, instanceOf(MapClientEntityImpl.class));
        assertThat(newInstance.getId(), is("my-id"));
    }

    @Test
    public void testCloneAsNewInstance() {
        MapClientEntity newInstance = CLONER.newInstance(MapClientEntity.class);
        newInstance.setId("my-id");
        newInstance.setClientId("a-client-id");
        newInstance.setAttribute("attr", Arrays.asList("aa", "bb", "cc"));

        MapClientEntity clonedInstance = CLONER.from(newInstance);
        assertThat(clonedInstance, instanceOf(MapClientEntityImpl.class));
        assertThat(clonedInstance.getId(), is("my-id"));
        assertThat(clonedInstance.getClientId(), is("a-client-id"));

        assertThat(clonedInstance.getAttributes(), not(sameInstance(newInstance.getAttributes())));
        assertThat(clonedInstance.getAttributes().keySet(), containsInAnyOrder("attr"));
        assertThat(clonedInstance.getAttributes().get("attr"), contains("aa", "bb", "cc"));
        assertThat(clonedInstance.getAttributes().get("attr"), not(sameInstance(newInstance.getAttributes().get("attr"))));

        assertThat(clonedInstance.getAuthenticationFlowBindingOverrides(), nullValue());
        assertThat(clonedInstance.getRegistrationToken(), nullValue());
    }

    @Test
    public void testCloneToExistingInstance() {
        MapClientEntity newInstance = CLONER.newInstance(MapClientEntity.class);
        newInstance.setId("my-id");
        newInstance.setClientId("a-client-id");
        newInstance.setAttribute("attr", Arrays.asList("aa", "bb", "cc"));
        MapProtocolMapperEntity pmm = new MapProtocolMapperEntityImpl();
        pmm.setId("pmm-id");
        pmm.setConfig(new HashMap<>());
        pmm.getConfig().put("key1", "value1");
        pmm.getConfig().put("key2", "value2");
        newInstance.setProtocolMapper("pmm-id", pmm);
        newInstance.setAttribute("attr", Arrays.asList("aa", "bb", "cc"));

        MapClientEntity clonedInstance = CLONER.newInstance(MapClientEntity.class);
        assertThat(CLONER.deepCloneNoId(newInstance, clonedInstance), sameInstance(clonedInstance));
        assertThat(clonedInstance, instanceOf(MapClientEntityImpl.class));
        clonedInstance.setId("my-id2");
        assertThat(clonedInstance.getId(), is("my-id2"));
        assertThat(clonedInstance.getClientId(), is("a-client-id"));

        assertThat(clonedInstance.getAttributes(), not(sameInstance(newInstance.getAttributes())));
        assertThat(clonedInstance.getAttributes().keySet(), containsInAnyOrder("attr"));
        assertThat(clonedInstance.getAttributes().get("attr"), contains("aa", "bb", "cc"));
        assertThat(clonedInstance.getAttributes().get("attr"), not(sameInstance(newInstance.getAttributes().get("attr"))));

        assertThat(clonedInstance.getProtocolMappers(), not(sameInstance(newInstance.getProtocolMappers())));
        assertThat(clonedInstance.getProtocolMapper("pmm-id"), not(sameInstance(newInstance.getProtocolMapper("pmm-id"))));
        assertThat(clonedInstance.getProtocolMapper("pmm-id"), equalTo(newInstance.getProtocolMapper("pmm-id")));
        assertThat(clonedInstance.getProtocolMapper("pmm-id").getConfig(), not(sameInstance(newInstance.getProtocolMapper("pmm-id").getConfig())));
        assertThat(clonedInstance.getProtocolMapper("pmm-id").getConfig(), equalTo(newInstance.getProtocolMapper("pmm-id").getConfig()));

        assertThat(clonedInstance.getAuthenticationFlowBindingOverrides(), nullValue());
        assertThat(clonedInstance.getRegistrationToken(), nullValue());
    }

    @Test
    public void testCloneToExistingInstanceDumb() {
        MapClientEntity newInstance = new MapClientEntityImpl();
        newInstance.setId("my-id");
        newInstance.setClientId("a-client-id");
        newInstance.setAttribute("attr", Arrays.asList("aa", "bb", "cc"));
        MapProtocolMapperEntity pmm = new MapProtocolMapperEntityImpl();
        pmm.setId("pmm-id");
        pmm.setConfig(new HashMap<>());
        pmm.getConfig().put("key1", "value1");
        pmm.getConfig().put("key2", "value2");
        newInstance.setProtocolMapper("pmm-id", pmm);
        newInstance.setAttribute("attr", Arrays.asList("aa", "bb", "cc"));

        MapClientEntity clonedInstance = CLONER.newInstance(MapClientEntity.class);
        assertThat(CLONER.deepCloneNoId(newInstance, clonedInstance), sameInstance(clonedInstance));
        assertThat(clonedInstance, instanceOf(MapClientEntityImpl.class));
        clonedInstance.setId("my-id2");
        assertThat(clonedInstance.getId(), is("my-id2"));
        assertThat(clonedInstance.getClientId(), is("a-client-id"));

        assertThat(clonedInstance.getAttributes(), not(sameInstance(newInstance.getAttributes())));
        assertThat(clonedInstance.getAttributes().keySet(), containsInAnyOrder("attr"));
        assertThat(clonedInstance.getAttributes().get("attr"), contains("aa", "bb", "cc"));
        assertThat(clonedInstance.getAttributes().get("attr"), not(sameInstance(newInstance.getAttributes().get("attr"))));

        assertThat(clonedInstance.getProtocolMappers(), not(sameInstance(newInstance.getProtocolMappers())));
        assertThat(clonedInstance.getProtocolMapper("pmm-id"), not(sameInstance(newInstance.getProtocolMapper("pmm-id"))));
        assertThat(clonedInstance.getProtocolMapper("pmm-id"), equalTo(newInstance.getProtocolMapper("pmm-id")));
        assertThat(clonedInstance.getProtocolMapper("pmm-id").getConfig(), not(sameInstance(newInstance.getProtocolMapper("pmm-id").getConfig())));
        assertThat(clonedInstance.getProtocolMapper("pmm-id").getConfig(), equalTo(newInstance.getProtocolMapper("pmm-id").getConfig()));

        assertThat(clonedInstance.getAuthenticationFlowBindingOverrides(), nullValue());
        assertThat(clonedInstance.getRegistrationToken(), nullValue());
    }
}
