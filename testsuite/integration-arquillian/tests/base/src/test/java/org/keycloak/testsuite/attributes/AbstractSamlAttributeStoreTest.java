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

import org.junit.After;
import org.junit.Before;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClientConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.attributes.AttributeStoreProvider;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Abstract class for tests for the {@link org.keycloak.storage.attributes.SAMLAttributeStoreProvider}
 */
public class AbstractSamlAttributeStoreTest extends AbstractAttributeStoreTest {

    protected static final String ATTR_LOOKUP_CLIENT = "attr-lookup-client";
    protected static final String ATTR_SRC_REALM = "attr-src";
    protected static final String ATTR_SRC_CLIENT = "attr-src-client";
    protected static final String ATTR_STORE_ID = KeycloakModelUtils.generateId();
    protected static final String TEST_USER_DN = "CN=test-user";

    protected static final CertHolder ATTR_SRC_SIGNING_CERT = generateCertificate("attr-src-sign");
    protected static final CertHolder ATTR_SRC_ENCRYPTION_CERT = generateCertificate("attr-src-enc");

    protected static final String SRC_CLIENT_SIGNING_CERT = PemUtils.encodeCertificate(ATTR_SRC_SIGNING_CERT.certificate);
    protected static final String SRC_CLIENT_SIGNING_KEY = PemUtils.encodeKey(ATTR_SRC_SIGNING_CERT.keyPair.getPrivate());
    protected static final String SRC_CLIENT_ENCRYPTION_CERT = PemUtils.encodeCertificate(ATTR_SRC_ENCRYPTION_CERT.certificate);
    protected static final String SRC_CLIENT_ENCRYPTION_KEY = PemUtils.encodeKey(ATTR_SRC_ENCRYPTION_CERT.keyPair.getPrivate());

    protected static final CertHolder ATTR_LOOKUP_SIGNING_CERT = generateCertificate("attr-lookup-sign");
    protected static final CertHolder ATTR_LOOKUP_ENCRYPTION_CERT = generateCertificate("attr-lookup-enc");

    protected static final String LOOKUP_CLIENT_SIGNING_CERT = PemUtils.encodeCertificate(ATTR_LOOKUP_SIGNING_CERT.certificate);
    protected static final String LOOKUP_CLIENT_SIGNING_KEY = PemUtils.encodeKey(ATTR_LOOKUP_SIGNING_CERT.keyPair.getPrivate());
    protected static final String LOOKUP_CLIENT_ENCRYPTION_CERT = PemUtils.encodeCertificate(ATTR_LOOKUP_ENCRYPTION_CERT.certificate);
    protected static final String LOOKUP_CLIENT_ENCRYPTION_KEY = PemUtils.encodeKey(ATTR_LOOKUP_ENCRYPTION_CERT.keyPair.getPrivate());

    @Before
    public void initialize() {
        super.initialize();
        initAttrSourceRealm();
        initAttrLookupReealm();
    }

    @After
    public void cleanup(){
        super.cleanup();
        realmsResouce().realm(ATTR_SRC_REALM).remove();
    }

    /**
     * Helper function to initialize the attribute source realm. Used as the target for the SAML attribute query requests
     */
    private void initAttrSourceRealm(){
        // create the realm
        realmsResouce().create(new RealmRepresentation(){{
            setEnabled(true);
            setRealm(ATTR_SRC_REALM);
        }});

        // create the attribute profile
        realmsResouce().realm(ATTR_SRC_REALM).users().userProfile().update(createAttributeProfile("test-attr-1", "test-attr-2", XASP_ATTRIBUTE));

        // create attribute lookup client
        realmsResouce().realm(ATTR_SRC_REALM).clients().create(
                ClientBuilder.create()
                        .clientId(ATTR_SRC_CLIENT)
                        .protocol("saml")
                        .attribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, SRC_CLIENT_ENCRYPTION_KEY)
                        .attribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, SRC_CLIENT_ENCRYPTION_CERT)
                        .attribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, SRC_CLIENT_SIGNING_KEY)
                        .attribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, SRC_CLIENT_SIGNING_CERT)

                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ISSUER, String.format("%s/realms/%s", OAuthClient.AUTH_SERVER_ROOT, ATTR_LOOKUP_REALM))
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_TARGET_AUDIENCE, String.format("%s/realms/%s", OAuthClient.AUTH_SERVER_ROOT, ATTR_LOOKUP_REALM))
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGNING_CERT, LOOKUP_CLIENT_SIGNING_CERT)
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPTION_CERT, LOOKUP_CLIENT_ENCRYPTION_CERT)

                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_SIGNED_REQ, "false")
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_ENCRYPTED_REQ, "false")

                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_DOC, "false")
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_ASSERTION, "false")
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPT_RESPONSE, "false")

                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_USER_LOOKUP_ATTRIBUTE, "")
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_FILTERS, "")
                        .attribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SUPPORTED, "true")
                        .enabled(true)
                        .build());

        // create test users
        realmsResouce().realm(ATTR_SRC_REALM).users().create(UserBuilder.create()
                .username(TEST_USER)
                .firstName("firstname")
                .lastName("lastname")
                .email("email@email.com")
                .password(TEST_USER_PASSWORD)
                .enabled(true)
                .build());
        realmsResouce().realm(ATTR_SRC_REALM).users().create(UserBuilder.create()
                .username(TEST_USER_NOT_SYNCED)
                .password(TEST_USER_NOT_SYNCED_PASSWORD)
                .enabled(true)
                .build());
    }

    /**
     * Additional configuration of the attribute lookup realm. Creates the SAML attribute query client and the SAML attribute
     * store provider instance.
     */
    private void initAttrLookupReealm(){
        // create SAML attribute query client
        realmsResouce().realm(ATTR_LOOKUP_REALM).clients().create(ClientBuilder.create()
                .clientId(ATTR_LOOKUP_CLIENT)
                .protocol("saml")
                .attribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, LOOKUP_CLIENT_ENCRYPTION_KEY)
                .attribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, LOOKUP_CLIENT_ENCRYPTION_CERT)
                .attribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, LOOKUP_CLIENT_SIGNING_KEY)
                .attribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, LOOKUP_CLIENT_SIGNING_CERT)
                .enabled(true)
                .build());

        // create SAML attribute store instance
        realmsResouce().realm(ATTR_LOOKUP_REALM).components().add(new ComponentRepresentation() {{
            setConfig(new MultivaluedHashMap<>() {{
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.SOAP_ENDPOINT, String.format("%s/realms/%s/protocol/saml/attributes", OAuthClient.AUTH_SERVER_ROOT, ATTR_SRC_REALM));
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.ATTRIBUTE_LOOKUP_CLIENT, ATTR_LOOKUP_CLIENT);
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.IDP_SIGNING_CERT, SRC_CLIENT_SIGNING_CERT);
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.IDP_ENCRYPTION_CERT, SRC_CLIENT_ENCRYPTION_CERT);
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.ISSUER, String.format("%s/realms/%s", OAuthClient.AUTH_SERVER_ROOT, ATTR_LOOKUP_REALM));
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.EXPECTED_ISSUER, String.format("%s/realms/%s", OAuthClient.AUTH_SERVER_ROOT, ATTR_SRC_REALM));
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.SUBJECT_FORMAT, "urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.SIGN_DOCUMENT, "false");
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.ENCRYPT_SUBJECT, "false");
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ASSERTION_SIGNATURE, "false");
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_DOCUMENT_SIGNATURE, "false");
                putSingle(SAMLAttributeQueryClientConfig.ProviderConfig.REQUIRE_ENCRYPTED_ASSERTION, "false");
            }});
            setId(ATTR_STORE_ID);
            setName("test-attribute-store");
            setProviderType(AttributeStoreProvider.class.getName());
            setProviderId("saml-attribute-store");
        }});

    }

    /**
     * Helper function to update the client with the given settings
     * @param realm the realm to update the provider in
     * @param clientId the client to update
     * @param settings the settings to update on the client
     */
    protected void updateClient(String realm, String clientId, Map<String, String> settings){
        ClientRepresentation client = adminClient.realm(realm).clients().findByClientId(clientId).stream().findFirst().orElseThrow();

        Map<String,String> config = client.getAttributes();
        config.putAll(settings);

        adminClient.realm(realm).clients().get(client.getId()).update(client);
    }
}