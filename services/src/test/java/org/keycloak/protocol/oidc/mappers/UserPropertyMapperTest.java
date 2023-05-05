/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.mappers;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alexander Schwartz
 */
public class UserPropertyMapperTest {

    @Test
    public void shouldAcceptValidPropertyNames() throws ProtocolMapperConfigException {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setConfig(new HashMap<>());
        mapping.getConfig().put(ProtocolMapperUtils.USER_ATTRIBUTE, "email");
        new UserPropertyMapper().validateConfig(null, null, null, mapping);
    }

    @Test
    public void shouldRejectInvalidPropertyNames() {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setConfig(new HashMap<>());
        mapping.getConfig().put(ProtocolMapperUtils.USER_ATTRIBUTE, "Email");
        ProtocolMapperConfigException exception = Assert.assertThrows(ProtocolMapperConfigException.class, () -> new UserPropertyMapper().validateConfig(null, null, null, mapping));
        assertThat(exception.getMessage(), startsWith("User property 'Email' does not exist"));
    }

}