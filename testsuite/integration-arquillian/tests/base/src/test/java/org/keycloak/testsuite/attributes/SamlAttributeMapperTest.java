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
package org.keycloak.testsuite.attributes;

import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.mappers.AttributeStoreMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Tests for the attribute store protocol mapper for the SAML attribute store
 */
public class SamlAttributeMapperTest extends AbstractSamlAttributeStoreTest {

    /**
     * Test the attribute mapper with a SAML attribute store
     */
    @Test
    public void testBasic() {
        // add the test attribute to the source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // create the protocol mapper instance
        addAttributeStoreProtocolMapper(ATTR_LOOKUP_REALM, "oidc-attribute-store-mapper", new HashMap<>(){{
            put(AttributeStoreMapper.CONFIG_ATTRIBUTE_POINTER, "/test-attr-1");
            put(AttributeStoreMapper.CONFIG_ATTRIBUTE_STORE_PROVIDER, getAttributeStoreProvider(ATTR_LOOKUP_REALM).getId());
            put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "test-attr-1");
            put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
            put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        }});

        Tokens tokens = login(TEST_USER, TEST_USER_PASSWORD);

        assertClaim(tokens.idToken, "test-attr-1", "test-value-1");
        assertClaim(tokens.accessToken, "test-attr-1", "test-value-1");
    }
}