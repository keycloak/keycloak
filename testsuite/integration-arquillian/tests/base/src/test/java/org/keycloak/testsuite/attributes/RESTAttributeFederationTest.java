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

import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.attributes.AttributeFederationProviderConfig;
import org.keycloak.storage.attributes.RESTAttributeStoreProviderConfig;
import org.keycloak.storage.attributes.mappers.UserAttributeMapperFactory;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.HashMap;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Tests for {@link org.keycloak.storage.attributes.AttributeFederationProvider} funcitonality with the {@link org.keycloak.storage.attributes.RESTAttributeStoreProvider}
 * provider
 */
public class RESTAttributeFederationTest extends AbstractRESTAttributeStoreTest {

    @Before
    public void initialize() {
        super.initialize();

        // create the attribute federation provider instance
        realmsResouce().realm(ATTR_LOOKUP_REALM).components().add(new ComponentRepresentation() {{
            setConfig(new MultivaluedHashMap<>() {{
                putSingle(AttributeFederationProviderConfig.CONFIG_ATTRIBUTE_STORE_PROVIDER, ATTR_STORE_ID);
                putSingle(AttributeFederationProviderConfig.CONFIG_GROUPS, getAttrSyncGroupPath());
            }});
            setId(ATTR_FED_ID);
            setName("test-attribute-federation");
            setProviderType(UserStorageProvider.class.getName());
            setProviderId("attributes");
        }});
    }

    /**
     * Test the REST api attribute request using a GET request
     */
    @Test
    public void testGET() {
        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/userinfo_endpoint");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", oauth.getUserInfoUrl()
        );
    }

    /**
     * Test the REST api attribute request when using a templated URL. Uses the XASP_ATTRIBUTE user attribute so you don't
     * have to update the realm attribute profile.
     *
     * Uses the JWKS keycloak endpoint as the target.
     */
    @Test
    public void testAttributeReplacement() {
        // set the attribute on the user to use to populate the templated URL
        setUserAttribute(ATTR_LOOKUP_REALM, TEST_USER, XASP_ATTRIBUTE, "protocol/openid-connect/certs");

        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/keys/0/kty");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // update the attribute store instance with the URL with a template string
        // URL (/realms/attr-lookup/${x509-dn}) should become (/realms/attr-lookup/protocol/openid-connect/certs)
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(RESTAttributeStoreProviderConfig.ProviderConfig.URL, String.format("%s/realms/%s/${%s}", OAuthClient.AUTH_SERVER_ROOT, ATTR_LOOKUP_REALM, XASP_ATTRIBUTE));
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "RSA",
                XASP_ATTRIBUTE, "protocol/openid-connect/certs"
        );
    }

    /**
     * Test that a user not in the specified group is not synced
     */
    @Test
    public void testUserNotSynced() {
        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/userinfo_endpoint");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        // ensure the test user belonging to the synced group has the new attribute populated
        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", oauth.getUserInfoUrl()
        );
        // ensure the test user not in the synced group does not have the new attribute
        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER_NOT_SYNCED);
    }
}