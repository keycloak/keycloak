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

package org.keycloak.authorization.fgap;

import java.util.Collection;

import org.keycloak.authorization.fgap.AdminPermissionsSchema.PermissionConfigOption;

import org.junit.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.EXTEND_CHILDREN;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.SCHEMA;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionConfigOptionTest {

    @Test
    public void testOnlyGroupsSupportExtendChildren() {
        Collection<PermissionConfigOption> groupOptions = SCHEMA.getPermissionConfigOptions(GROUPS_RESOURCE_TYPE);

        assertEquals(1, groupOptions.size());
        assertEquals(EXTEND_CHILDREN, groupOptions.iterator().next().name());
        assertTrue(SCHEMA.getPermissionConfigOptions(USERS_RESOURCE_TYPE).isEmpty());
        assertTrue(SCHEMA.getPermissionConfigOptions(null).isEmpty());
    }

    @Test
    public void testExtendChildrenOnlyAcceptsExactBooleans() {
        PermissionConfigOption option = SCHEMA.getPermissionConfigOptions(GROUPS_RESOURCE_TYPE).iterator().next();

        assertTrue(option.isValid("true"));
        assertTrue(option.isValid("false"));
        assertFalse(option.isValid("TRUE"));
        assertFalse(option.isValid("yes"));
        assertFalse(option.isValid(""));
        assertFalse(option.isValid(null));
    }

    @Test
    public void testExtendChildrenDefaultIsDisabled() {
        PermissionConfigOption option = SCHEMA.getPermissionConfigOptions(GROUPS_RESOURCE_TYPE).iterator().next();

        assertTrue(option.isDefault(null));
        assertTrue(option.isDefault("false"));
        assertFalse(option.isDefault("true"));
    }
}
