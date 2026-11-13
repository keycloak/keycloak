/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;

import org.keycloak.authorization.jpa.entities.ResourceAttributeEntity;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.store.ResourceAdapter;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AttributeValidationTest {

    @Test
    public void testRoleAttributeValidation() {
        RoleEntity entity = new RoleEntity();
        RoleAdapter adapter = new RoleAdapter(null, null, null, entity);

        String tooLongName = "a".repeat(RoleAttributeEntity.NAME_MAX_LENGTH + 1);
        String tooLongValue = "v".repeat(RoleAttributeEntity.VALUE_MAX_LENGTH + 1);

        ModelValidationException ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute(tooLongName, "val"));
        assertEquals("Attribute name must not exceed " + RoleAttributeEntity.NAME_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute("key", tooLongValue));
        assertEquals("Attribute value must not exceed " + RoleAttributeEntity.VALUE_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(tooLongName, List.of("val")));
        assertEquals("Attribute name must not exceed " + RoleAttributeEntity.NAME_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", List.of(tooLongValue)));
        assertEquals("Attribute value must not exceed " + RoleAttributeEntity.VALUE_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute(null, "val"));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute("key", null));
        assertEquals("Attribute value must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, List.of("val")));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, null));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", Collections.singletonList(null)));
        assertEquals("Attribute value must not be null", ex.getMessage());
    }

    @Test
    public void testGroupAttributeValidation() {
        GroupEntity entity = new GroupEntity();
        GroupAdapter adapter = new GroupAdapter(null, null, null, entity);

        String tooLongName = "a".repeat(GroupAttributeEntity.NAME_MAX_LENGTH + 1);
        String tooLongValue = "v".repeat(GroupAttributeEntity.VALUE_MAX_LENGTH + 1);

        ModelValidationException ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute(tooLongName, "val"));
        assertEquals("Attribute name must not exceed " + GroupAttributeEntity.NAME_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute("key", tooLongValue));
        assertEquals("Attribute value must not exceed " + GroupAttributeEntity.VALUE_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(tooLongName, List.of("val")));
        assertEquals("Attribute name must not exceed " + GroupAttributeEntity.NAME_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", List.of(tooLongValue)));
        assertEquals("Attribute value must not exceed " + GroupAttributeEntity.VALUE_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute(null, "val"));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setSingleAttribute("key", null));
        assertEquals("Attribute value must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, List.of("val")));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, null));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", Collections.singletonList(null)));
        assertEquals("Attribute value must not be null", ex.getMessage());
    }

    @Test
    public void testResourceAttributeValidation() {
        ResourceEntity entity = new ResourceEntity();
        org.keycloak.authorization.store.StoreFactory storeFactory = (org.keycloak.authorization.store.StoreFactory) java.lang.reflect.Proxy.newProxyInstance(
                org.keycloak.authorization.store.StoreFactory.class.getClassLoader(),
                new Class<?>[]{org.keycloak.authorization.store.StoreFactory.class},
                (proxy, method, args) -> "isReadOnly".equals(method.getName()) ? Boolean.FALSE : null
        );
        ResourceAdapter adapter = new ResourceAdapter(entity, null, storeFactory);

        String tooLongName = "a".repeat(ResourceAttributeEntity.NAME_MAX_LENGTH + 1);
        String tooLongValue = "v".repeat(ResourceAttributeEntity.VALUE_MAX_LENGTH + 1);

        ModelValidationException ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(tooLongName, List.of("val")));
        assertEquals("Attribute name must not exceed " + ResourceAttributeEntity.NAME_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", List.of(tooLongValue)));
        assertEquals("Attribute value must not exceed " + ResourceAttributeEntity.VALUE_MAX_LENGTH + " characters", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, List.of("val")));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute(null, null));
        assertEquals("Attribute name must not be null", ex.getMessage());

        ex = assertThrows(ModelValidationException.class, () ->
                adapter.setAttribute("key", Collections.singletonList(null)));
        assertEquals("Attribute value must not be null", ex.getMessage());
    }
}
