/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.exportimport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationExportTest extends AbstractOrganizationTest {

    @Test
    public void testPartialExport() {
        createOrganization();
        assertPartialExportImport(false, false);
        assertPartialExportImport(true, false);
        assertPartialExportImport(true, true);
        assertPartialExportImport(false, true);
    }

    private void assertPartialExportImport(boolean exportGroupsAndRoles, boolean exportClients) {
        RealmRepresentation export = realm.admin().partialExport(exportGroupsAndRoles, exportClients);
        assertTrue(Optional.ofNullable(export.getOrganizations()).orElse(List.of()).isEmpty());
        assertTrue(Optional.ofNullable(export.getIdentityProviders()).orElse(List.of()).stream().noneMatch(idp -> Objects.nonNull(idp.getOrganizationId())));
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setUsers(export.getUsers());
        rep.setClients(export.getClients());
        rep.setRoles(export.getRoles());
        rep.setIdentityProviders(export.getIdentityProviders());
        rep.setGroups(export.getGroups());
        realm.admin().partialImport(rep).close();
    }
}
