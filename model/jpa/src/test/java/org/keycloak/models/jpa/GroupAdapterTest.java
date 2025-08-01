/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.jpa.testhelper.GroupEntityBuilder;
import org.keycloak.jpa.testhelper.JpaUnitTestExecutionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.jpa.entities.GroupEntity;

import jakarta.persistence.EntityManager;

@SuppressWarnings("resource")
public class GroupAdapterTest {

    private static final String GROUP_01_ID = "d18c2861-e8d6-4bbf-ab21-24f6026ffb78";
    private static final String GROUP_02_ID = "06e55895-6f22-4d76-a269-decdd30778b8";

    @ClassRule
    public static JpaUnitTestExecutionContext testContext = new JpaUnitTestExecutionContext();

    @Test
    public void test_setAttribute() {
        initTestGroups(testContext.getEntityManager());

        GroupAdapter groupAdapter = getGroupAdapter(GROUP_01_ID);

        assertThat(groupAdapter.getAttributes().get("attr0"), nullValue());
        assertThat(groupAdapter.getAttributes().get("attr1"), hasItem("123"));
        assertThat(groupAdapter.getAttributes().get("attr2"), hasItems("ABC", "DEF"));

        groupAdapter.setAttribute("attr1", List.of("456"));
        groupAdapter.setAttribute("attr3", List.of("alpha", "beta", "alpha", "delta", "alpha"));

        groupAdapter = getGroupAdapter(GROUP_01_ID);
        assertThat(groupAdapter.getAttributes().get("attr1"), hasItem("456"));
        assertThat(groupAdapter.getAttributes().get("attr3"), contains("alpha", "beta", "alpha", "delta", "alpha"));

        groupAdapter.setAttribute("attr1", new ArrayList<>());
        groupAdapter.setAttribute("attr3", List.of("alpha", "beta", "gamma", "delta", "alpha"));

        groupAdapter = getGroupAdapter(GROUP_01_ID);
        assertThat(groupAdapter.getAttributes().get("attr1"), nullValue());
        assertThat(groupAdapter.getAttributes().get("attr3"), contains("alpha", "beta", "gamma", "delta", "alpha"));

        groupAdapter = getGroupAdapter(GROUP_01_ID);
        groupAdapter.setAttribute("attr3", null);
        assertThat(groupAdapter.getAttributes().get("attr3"), nullValue());
    }

    private GroupAdapter getGroupAdapter(String groupId) {
        GroupEntity groupEntity = testContext.findGroupEntityById(groupId);
        return new GroupAdapter(testContext.getKeycloakSession(), testContext.getRealm(), testContext.getEntityManager(),
                groupEntity);
    }

    private void initTestGroups(EntityManager entityManager) {
        GroupEntity groupEntity;

        entityManager.createQuery(String.format("DELETE FROM %s entity", GroupEntity.class.getSimpleName())).executeUpdate();

        groupEntity = GroupEntityBuilder.create() //
                .id(GROUP_01_ID) //
                .name("group01") //
                .type(GroupModel.Type.REALM) //
                .addAttribute("attr1", "123") //
                .addAttribute("attr2", "ABC") //
                .addAttribute("attr2", "DEF") //
                .build();
        entityManager.persist(groupEntity);

        groupEntity = GroupEntityBuilder.create() //
                .id(GROUP_02_ID) //
                .name("group02") //
                .type(GroupModel.Type.REALM) //
                .build();
        entityManager.persist(groupEntity);
    }
}
