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
package org.keycloak.representations.idm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrganizationRepresentationTest {

    @Test
    public void organizationRolesAreRepresentedExplicitly() {
        RoleRepresentation defaultRole = new RoleRepresentation();
        defaultRole.setName("member");
        RoleRepresentation customRole = new RoleRepresentation();
        customRole.setName("admin");
        OrganizationRepresentation organization = new OrganizationRepresentation();

        organization.setDefaultRole(defaultRole);
        organization.addRole(customRole);

        assertEquals("member", organization.getDefaultRole().getName());
        assertEquals(Collections.singletonList(customRole), organization.getRoles());

        organization.setRoles(Collections.singletonList(defaultRole));
        assertEquals(Collections.singletonList(defaultRole), organization.getRoles());
    }

    @Test
    public void memberOrganizationRolesAreRepresentedSeparatelyFromRealmAndClientRoles() {
        MemberRepresentation member = new MemberRepresentation();

        member.addOrganizationRole("member");
        member.setOrganizationRoles(Arrays.asList("member", "admin"));

        assertEquals(Arrays.asList("member", "admin"), member.getOrganizationRoles());
    }

    @Test
    public void roleCompositesCanReferenceOrganizationRoles() {
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();

        composites.setOrganization(new HashSet<>(Arrays.asList("member", "admin")));

        assertEquals(new HashSet<>(Arrays.asList("member", "admin")), composites.getOrganization());
    }
}
