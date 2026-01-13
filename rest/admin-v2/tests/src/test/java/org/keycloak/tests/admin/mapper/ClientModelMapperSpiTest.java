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

import org.junit.jupiter.api.Test;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ClientModelMapperFactory;
import org.keycloak.models.mapper.ClientModelMapperSpi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.typeCompatibleWith;

class ClientModelMapperSpiTest {

    @Test
    void testIsInternal() {
        ClientModelMapperSpi spi = new ClientModelMapperSpi();
        
        assertThat(spi.isInternal(), is(true));
    }

    @Test
    void testGetName() {
        ClientModelMapperSpi spi = new ClientModelMapperSpi();
        
        assertThat(spi.getName(), is("client-model-mapper"));
    }

    @Test
    void testGetProviderClass() {
        ClientModelMapperSpi spi = new ClientModelMapperSpi();
        
        assertThat(spi.getProviderClass(), typeCompatibleWith(ClientModelMapper.class));
    }

    @Test
    void testGetProviderFactoryClass() {
        ClientModelMapperSpi spi = new ClientModelMapperSpi();
        
        assertThat(spi.getProviderFactoryClass(), typeCompatibleWith(ClientModelMapperFactory.class));
    }
}
