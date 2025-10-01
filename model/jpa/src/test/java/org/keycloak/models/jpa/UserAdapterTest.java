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
import org.keycloak.jpa.testhelper.UserEntityBuilder;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;

import jakarta.persistence.EntityManager;

@SuppressWarnings("resource")
public class UserAdapterTest {

    private static final String USER_01_ID = "183fea46-a1be-447c-b622-643c11c7fa6b";
    private static final String USER_02_ID = "ea4b3c09-b1e5-4502-8389-5c84ca23c093";

    @ClassRule
    public static JpaUnitTestExecutionContext testContext = new JpaUnitTestExecutionContext();

    @Test
    public void test_setAttribute() {
        initTestUsers(testContext.getEntityManager());
        UserAdapter userAdapter = getUserAdapter(USER_02_ID);

        assertThat(userAdapter.getAttributes().get(UserModel.FIRST_NAME), hasItem("Oliver"));
        assertThat(userAdapter.getAttributes().get("phone"), hasItem("123"));
        assertThat(userAdapter.getEmail(), nullValue());

        userAdapter.setAttribute("email", List.of("oliver.hardy@comedy.org")); // explicitly check special attribute (aka
                                                                               // property)
        userAdapter.setAttribute("phone", List.of("789"));
        userAdapter.setAttribute("code", List.of("alpha", "beta", "alpha", "delta", "alpha"));

        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getEmail(), is("oliver.hardy@comedy.org"));
        assertThat(userAdapter.getAttributes().get("phone"), hasItem("789"));
        assertThat(userAdapter.getAttributes().get("code"), contains("alpha", "beta", "alpha", "delta", "alpha"));

        userAdapter.setAttribute("code", List.of("alpha", "beta", "alpha", "alpha", "delta", "alpha"));
        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getAttributes().get("code"), contains("alpha", "beta", "alpha", "alpha", "delta", "alpha"));

        userAdapter.setAttribute("code", List.of("alpha", "beta", "delta"));
        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getAttributes().get("code"), contains("alpha", "beta", "delta"));

        userAdapter.setAttribute("code", new ArrayList<>());
        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getAttributes().get("code"), nullValue());

        userAdapter.setAttribute("phone", null);
        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getAttributes().get("phone"), nullValue());

        userAdapter.setAttribute(UserModel.USERNAME, List.of("cc"));
        userAdapter.setAttribute(UserModel.FIRST_NAME, List.of("Charlie"));
        userAdapter.setAttribute(UserModel.LAST_NAME, List.of("Chaplin"));
        userAdapter = getUserAdapter(USER_02_ID);
        assertThat(userAdapter.getUsername(), is("cc"));
        assertThat(userAdapter.getFirstName(), is("Charlie"));
        assertThat(userAdapter.getLastName(), is("Chaplin"));
    }

    private UserAdapter getUserAdapter(String userId) {
        UserEntity userEntity = testContext.findUserEntityById(userId);
        return new UserAdapter(testContext.getKeycloakSession(), testContext.getRealm(), testContext.getEntityManager(),
                userEntity);
    }

    private void initTestUsers(EntityManager entityManager) {
        UserEntity userEntity;

        entityManager.createQuery(String.format("DELETE FROM %s entity", UserEntity.class.getSimpleName())).executeUpdate();

        userEntity = UserEntityBuilder.create().id(USER_01_ID).username("user01").firstName("Stan").lastName("laurel").build();
        entityManager.persist(userEntity);

        userEntity = UserEntityBuilder.create() //
                .id(USER_02_ID) //
                .username("user02") //
                .firstName("Oliver") //
                .lastName("Hardy") //
                .addAttribute("phone", "123") //
                .addAttribute("department", "research") //
                .build();
        entityManager.persist(userEntity);
    }
}
