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
import org.keycloak.jpa.testhelper.JpaUnitTestExecutionContext;
import org.keycloak.jpa.testhelper.RoleEntityBuilder;
import org.keycloak.models.jpa.entities.RoleEntity;

import jakarta.persistence.EntityManager;

@SuppressWarnings("resource")
public class RoleAdapterTest {

    private static final String ROLE_01_ID = "70fece75-4c32-4fd2-8efc-0ca598b830fc";
    private static final String ROLE_02_ID = "8e67cc4f-1256-4bb2-9f9b-79371212755b";

    @ClassRule
    public static JpaUnitTestExecutionContext testContext = new JpaUnitTestExecutionContext();

    @Test
    public void test_setAttribute() {
        initTestRoles(testContext.getEntityManager());

        RoleAdapter roleAdapter = getRoleAdapter(ROLE_01_ID);

        assertThat(roleAdapter.getAttributes().get("attr0"), nullValue());
        assertThat(roleAdapter.getAttributes().get("attr1"), hasItem("123"));
        assertThat(roleAdapter.getAttributes().get("attr2"), hasItems("ABC", "DEF"));

        roleAdapter.setAttribute("attr1", List.of("456"));
        roleAdapter.setAttribute("attr3", List.of("alpha", "beta", "alpha", "delta", "alpha"));

        roleAdapter = getRoleAdapter(ROLE_01_ID);
        assertThat(roleAdapter.getAttributes().get("attr1"), hasItem("456"));
        assertThat(roleAdapter.getAttributes().get("attr3"), contains("alpha", "beta", "alpha", "delta", "alpha"));

        roleAdapter.setAttribute("attr1", new ArrayList<>());
        roleAdapter.setAttribute("attr3", List.of("alpha", "beta", "gamma", "delta", "alpha"));

        roleAdapter = getRoleAdapter(ROLE_01_ID);
        assertThat(roleAdapter.getAttributes().get("attr1"), nullValue());
        assertThat(roleAdapter.getAttributes().get("attr3"), contains("alpha", "beta", "gamma", "delta", "alpha"));

        roleAdapter = getRoleAdapter(ROLE_01_ID);
        roleAdapter.setAttribute("attr3", null);
        assertThat(roleAdapter.getAttributes().get("attr3"), nullValue());
    }

    private RoleAdapter getRoleAdapter(String roleId) {
        RoleEntity roleEntity = testContext.findRoleEntityById(roleId);
        return new RoleAdapter(testContext.getKeycloakSession(), testContext.getRealm(), testContext.getEntityManager(),
                roleEntity);
    }

    private void initTestRoles(EntityManager entityManager) {
        RoleEntity roleEntity;

        entityManager.createQuery(String.format("DELETE FROM %s entity", RoleEntity.class.getSimpleName())).executeUpdate();

        roleEntity = RoleEntityBuilder.create().id(ROLE_01_ID) //
                .name("role01") //
                .realmId(testContext.getRealm().getId()) //
                .addAttribute("attr1", "123") //
                .addAttribute("attr2", "ABC") //
                .addAttribute("attr2", "DEF") //
                .build();
        entityManager.persist(roleEntity);

        roleEntity = RoleEntityBuilder.create() //
                .id(ROLE_02_ID) //
                .name("role02") //
                .realmId(testContext.getRealm().getId()) //
                .build();
        entityManager.persist(roleEntity);
    }
}
