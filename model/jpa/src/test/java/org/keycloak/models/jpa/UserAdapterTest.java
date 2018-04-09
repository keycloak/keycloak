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

package org.keycloak.models.jpa;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


/**
 * @author <a href="mailto:urs.honegger@starmind.com">Urs Honegger</a>
 */
public class UserAdapterTest {

    UserAdapter userAdapter = null;

    @Before
    public void setUp() {
        EntityManager mockedEm = mock(EntityManager.class);
        doNothing().when(mockedEm).persist(anyObject());
        doReturn(mock(Query.class)).when(mockedEm).createNamedQuery(anyString());

        UserAttributeEntity firstValue = new UserAttributeEntity();
        firstValue.setId("v1");
        firstValue.setName("foo");
        firstValue.setValue("aValue");

        UserAttributeEntity secondValue = new UserAttributeEntity();
        secondValue.setId("v2");
        secondValue.setName("foo");
        secondValue.setValue("anotherValue");

        ArrayList<UserAttributeEntity> attributes = new ArrayList<>();
        attributes.add(firstValue);
        attributes.add(secondValue);

        UserEntity user = new UserEntity();
        user.setAttributes(attributes);

        userAdapter = new UserAdapter(null, null, mockedEm, user);
    }

    @After
    public void tearDown() {
        userAdapter = null;
    }

    @Test
    public void setSingleNonNullAttribute() {
        userAdapter.setSingleAttribute("foo", "bar");
        Map<String, List<String>> attributes = userAdapter.getAttributes();
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(1, foo.size());
        assertEquals("bar", foo.get(0));
    }

    @Test
    public void setNonNullAttributes() {
        userAdapter.setAttribute("foo", Arrays.asList("bar", "baz"));
        Map<String, List<String>> attributes = userAdapter.getAttributes();
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(2, foo.size());
        assertEquals("baz", foo.get(1));
    }

    @Test
    public void setSingleNullAttribute() {
        userAdapter.setSingleAttribute("foo", null);
        Map<String, List<String>> attributes = userAdapter.getAttributes();
        assertFalse(attributes.containsKey("foo"));
    }

    @Test
    public void setNullAttributes() {
        userAdapter.setAttribute("foo", Arrays.asList(null, null));
        Map<String, List<String>> attributes = userAdapter.getAttributes();
        assertFalse(attributes.containsKey("foo"));
    }

    @Test
    public void setAttributesWithValuesAndNull() {
        userAdapter.setAttribute("foo", Arrays.asList(null, "bar", "baz", "aValue", null));
        Map<String, List<String>> attributes = userAdapter.getAttributes();
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(3, foo.size());
        assertEquals(Arrays.asList("bar", "baz", "aValue"), foo);
    }
}
