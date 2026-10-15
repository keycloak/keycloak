/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.tests.admin.partialimport;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for the partial import endpoint in admin client.  Also tests the
 * server side functionality of each resource along with "fail, skip, overwrite"
 * functions.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class PartialImportGroupTest extends AbstractPartialImportTest {

    @ParameterizedTest
    @CsvSource({
            "plain, SKIP, false",
            "plain, OVERWRITE, false",
            "plain, FAIL, false",
            "a/b, SKIP, false",
            "a/b, OVERWRITE, false",
            "a/b, FAIL, false",
            "plain, SKIP, true",
            "plain, OVERWRITE, true",
            "plain, FAIL, true",
            "a/b, SKIP, true",
            "a/b, OVERWRITE, true",
            "a/b, FAIL, true"
    })
    public void testImportGroupWithOptionalPath(String name, PartialImportRepresentation.Policy policy, boolean includePath) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        if (includePath) {
            group.setPath("/" + name);
        }
        group.setAttributes(Map.of("attribute", List.of("original")));
        piRep.setGroups(List.of(group));
        piRep.setIfResourceExists(policy.toString());

        String groupId;
        try (Response response = managedRealm.admin().partialImport(piRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
            PartialImportResults results = response.readEntity(PartialImportResults.class);
            assertThat(results.getAdded(), equalTo(1));
            groupId = results.getResults().iterator().next().getId();
            assertThat(groupId, notNullValue());
        }
        assertThat(managedRealm.admin().groups().group(groupId).toRepresentation().getName(), equalTo(name));

        group.setAttributes(Map.of("attribute", List.of("updated")));
        try (Response response = managedRealm.admin().partialImport(piRep)) {
            if (policy == PartialImportRepresentation.Policy.FAIL) {
                assertThat(response.getStatus(), equalTo(Response.Status.CONFLICT.getStatusCode()));
                ErrorRepresentation errorRep = response.readEntity(ErrorRepresentation.class);
                assertThat(errorRep.getErrorMessage(),
                        equalTo("Group '" + (includePath ? "/" + name : name) + "' already exists"));
            } else {
                assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
                PartialImportResults results = response.readEntity(PartialImportResults.class);
                if (policy == PartialImportRepresentation.Policy.SKIP) {
                    assertThat(results.getSkipped(), equalTo(1));
                    assertThat(results.getResults().iterator().next().getId(), equalTo(groupId));
                } else {
                    assertThat(results.getOverwritten(), equalTo(1));
                    groupId = results.getResults().iterator().next().getId();
                    assertThat(groupId, notNullValue());
                }
            }
        }

        assertThat(managedRealm.admin().groups().groups(), hasSize(1));
        GroupRepresentation importedGroup = managedRealm.admin().groups().group(groupId).toRepresentation();
        assertThat(importedGroup.getName(), equalTo(name));
        assertThat(importedGroup.getAttributes().get("attribute"),
                equalTo(List.of(policy == PartialImportRepresentation.Policy.OVERWRITE ? "updated" : "original")));
    }

    @Test
    public void testAddGroupsFail() {
        addGroups();
        testFail();
    }


    @Test
    public void testAddGroupsSkip() {
        addGroups();
        testSkip();
    }

    @Test
    @DatabaseTest
    public void testAddGroupsOverwrite() {
        addGroups();
        testOverwrite();
    }

    @Test
    public void testAddSingleGroupOverwrite() {
        addGroups(1);
        testOverwrite(1);
    }
}
