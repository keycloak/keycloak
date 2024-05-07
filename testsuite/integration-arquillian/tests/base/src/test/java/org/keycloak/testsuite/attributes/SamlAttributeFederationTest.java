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
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.attributes.AttributeFederationProviderConfig;
import org.keycloak.storage.attributes.mappers.*;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.*;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClientConfig;

import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Tests for {@link org.keycloak.storage.attributes.AttributeFederationProvider} funcitonality with the {@link org.keycloak.storage.attributes.SAMLAttributeStoreProvider}
 * provider
 */
public class SamlAttributeFederationTest extends AbstractSamlAttributeStoreTest {

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
     * Test the SAML attribute query functionality with no signing or encryption
     */
    @Test
    public void testBasic() {
        // add an attribute to the source realm to be queried
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test the hardcoded attribute federation mapper
     */
    @Test
    public void testBasicHardcodedMapper() {
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "hardcoded-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(HardcodedAttributeMapperFactory.CONFIG_ATTRIBUTE_NAME, "test-attr-1");
            putSingle(HardcodedAttributeMapperFactory.CONFIG_ATTRIBUTE_VALUE, "test-value-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test the script attribute federation mapper
     */
    @Test
    public void testScriptAttributeMapper() {
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        addAttributeFedMapper(ATTR_LOOKUP_REALM, "script-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(ScriptAttributeMapperFactory.CONFIG_SCRIPT_SOURCE, "exports = sourceAttributes[\"test-attr-1\"]");
            putSingle(ScriptAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test SAML attribute query functionality with encryption required on both request and response.
     */
    @Test
    public void testEncrypted(){
        // add the test attribute to the source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // update the attribute store provider to encrypt requests and enforce encryption
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(SAMLAttributeQueryClientConfig.ProviderConfig.ENCRYPT_SUBJECT, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ENCRYPTED_ASSERTION, "true");
        }});
        // update the SAML attribute query client to encrypt responses and enforce encryption
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPT_RESPONSE, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_ENCRYPTED_REQ, "true");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test SAML attribute query functionality with signatures required for the response assertions
     */
    @Test
    public void testSignedAssertion(){
        // add the test attribute to the source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // configure the attribute store provider to sign the request and require signed assertions
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(SAMLAttributeQueryClientConfig.ProviderConfig.SIGN_DOCUMENT, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ASSERTION_SIGNATURE, "true");
        }});
        // configure the SAML client to sign assertions and required signed requests
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_ASSERTION, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_SIGNED_REQ, "true");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test SAML attribute query functionality with signatures required on the request and response documents.
     */
    @Test
    public void testSignedDoc(){
        // add the test attribute to the source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // configure the attribute store provider to sign the request document and enforce response signatures
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(SAMLAttributeQueryClientConfig.ProviderConfig.SIGN_DOCUMENT, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_DOCUMENT_SIGNATURE, "true");
        }});

        // configure the SAML client to sign response documents and enforce signatures on requests
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_DOC, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_SIGNED_REQ, "true");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test SAML attribute query functionality with all security options enabled
     * (encryption and signatures on requests and responses)
     */
    @Test
    public void testAllSecurityOptions(){
        // add test attribute to source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // create attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // configure security requirements for attribute store provider
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(SAMLAttributeQueryClientConfig.ProviderConfig.SIGN_DOCUMENT, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.ENCRYPT_SUBJECT, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ASSERTION_SIGNATURE, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_DOCUMENT_SIGNATURE, "true");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ENCRYPTED_ASSERTION, "true");
        }});
        // configure security requirements on SAML client
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_DOC, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_ASSERTION, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPT_RESPONSE, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_SIGNED_REQ, "true");
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_ENCRYPTED_REQ, "true");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test the SAML attribute query client filters.
     */
    @Test
    public void testFilter(){
        // attribute included in filter
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");
        // attribute not included in filter
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "bad-attr", "bad-value");

        // configure attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});
        // set the attribute filter on the SAML client
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_FILTERS, "[\"test-attr-.*\"]");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        // only attributes starting with "test-attr-" should be synced
        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
    }

    /**
     * Test SAML2 x509 attribute query profile compliance. In this scenario, the subject of the attribute query request
     * is the DN from the users x509 certificate.
     */
    @Test
    public void testX509() {
        // set the test attribute on the source user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");

        // set the x509 DN as an attribute on the source and destination user
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, XASP_ATTRIBUTE, TEST_USER_DN);
        setUserAttribute(ATTR_LOOKUP_REALM, TEST_USER, XASP_ATTRIBUTE, TEST_USER_DN);

        // create the attribute mapper instance
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        // configure the attribute store provider to use the XASP_ATTRIBUTE of the user as the subject of the SAML attribute
        // query request
        updateAttributeStoreProvider(ATTR_LOOKUP_REALM, new HashMap<>(){{
            put(SAMLAttributeQueryClientConfig.ProviderConfig.SUBJECT_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName");
            put(SAMLAttributeQueryClientConfig.ProviderConfig.SUBJECT_ATTRIBUTE, XASP_ATTRIBUTE);
        }});
        // configure the SAML client to lookup users based on the XASP_ATTRIBUTE value instead of standard username
        updateClient(ATTR_SRC_REALM, ATTR_SRC_CLIENT, new HashMap<>(){{
            put(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_USER_LOOKUP_ATTRIBUTE, XASP_ATTRIBUTE);
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1",
                XASP_ATTRIBUTE, TEST_USER_DN
        );
    }

    /**
     * Test to ensure only users in the specified groups are synced
     */
    @Test
    public void testUserNotSynced() {
        // add test attribute to source users
        setUserAttribute(ATTR_SRC_REALM, TEST_USER, "test-attr-1", "test-value-1");
        setUserAttribute(ATTR_SRC_REALM, TEST_USER_NOT_SYNCED, "test-attr-1", "test-value-1");

        // create the attribute mapper
        addAttributeFedMapper(ATTR_LOOKUP_REALM, "user-attribute-mapper", new MultivaluedHashMap<>(){{
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER, "/test-attr-1");
            putSingle(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST, "test-attr-1");
        }});

        triggerAttributeSync(ATTR_LOOKUP_REALM, ATTR_FED_ID, true);

        // attribute should be synced to test user
        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER,
                "test-attr-1", "test-value-1"
        );
        // attribute should not be synced for this user
        assertAttributes(ATTR_LOOKUP_REALM, TEST_USER_NOT_SYNCED);
    }
}