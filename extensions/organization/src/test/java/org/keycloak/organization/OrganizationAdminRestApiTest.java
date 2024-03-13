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

package org.keycloak.organization;

import java.util.List;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;

public class OrganizationAdminRestApiTest {

    private static final TypeRef<List<OrganizationRepresentation>> ORGANIZATION_LIST_TYPE_DEF = new TypeRef<List<OrganizationRepresentation>>() {};
    private static final String BASE_SERVER_URL = "http://localhost:8180";

    private Keycloak kcClient;
    private String accessToken;
    private RequestSpecification restAssured;

    @BeforeEach
    public void onBefore() {
        kcClient = Keycloak.getInstance(BASE_SERVER_URL, "master", "admin", "admin", "admin-cli");
        accessToken = kcClient.tokenManager().getAccessTokenString();
        restAssured = RestAssured.given().auth().oauth2(accessToken);
    }

    @AfterEach
    public void onAfter() {
        if (kcClient != null) {
            kcClient.close();
        }
    }

    @Test
    public void testCreate() {
        OrganizationRepresentation org = new OrganizationRepresentation();

        org.setName("acme");

        restAssured.body(org)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_SERVER_URL.concat("/admin/realms/master/organization"))
                .then()
                .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testDelete() {
        OrganizationRepresentation org = new OrganizationRepresentation();

        org.setId("1");

        restAssured.delete(BASE_SERVER_URL.concat("/admin/realms/master/organization/{id}"), org.getId())
                .then()
                .statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testGet() {
        List<OrganizationRepresentation> organizations = restAssured
                .get(BASE_SERVER_URL.concat("/admin/realms/master/organization"))
                .as(ORGANIZATION_LIST_TYPE_DEF);
        Assertions.assertFalse(organizations.isEmpty());
    }
}
