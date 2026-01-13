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

package org.keycloak.tests.admin.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.OIDCClientModelMapper;
import org.keycloak.models.mapper.OIDCClientModelMapperFactory;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
class OIDCClientModelMapperFactoryTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakSessionFactory sessionFactory;

    private OIDCClientModelMapperFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OIDCClientModelMapperFactory();
    }

    @Test
    void testGetId() {
        assertThat(factory.getId(), is(OIDCClientRepresentation.PROTOCOL));
        assertThat(factory.getId(), is("openid-connect"));
    }

    @Test
    void testCreate() {
        ClientModelMapper mapper = factory.create(session);

        assertThat(mapper, notNullValue());
        assertThat(mapper, instanceOf(OIDCClientModelMapper.class));
    }

    @Test
    void testPostInitDoesNotThrow() {
        // Verify postInit doesn't throw any exception
        factory.postInit(sessionFactory);
    }

    @Test
    void testCloseDoesNotThrow() {
        // Verify close doesn't throw any exception
        factory.close();
    }

    @Test
    void testCreateReturnsNewInstanceEachTime() {
        ClientModelMapper mapper1 = factory.create(session);
        ClientModelMapper mapper2 = factory.create(session);

        assertThat(mapper1, notNullValue());
        assertThat(mapper2, notNullValue());
        // Each call should return a new instance
        assertThat(mapper1 == mapper2, is(false));
    }
}
