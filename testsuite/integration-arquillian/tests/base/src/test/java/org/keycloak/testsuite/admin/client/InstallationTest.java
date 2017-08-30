/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test getting the installation/configuration files for OIDC and SAML.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class InstallationTest extends AbstractClientTest {

    private static final String OIDC_NAME = "oidcInstallationClient";
    private static final String OIDC_NAME_BEARER_ONLY_NAME = "oidcInstallationClientBearerOnly";
    private static final String OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME = "oidcInstallationClientBearerOnlyWithAuthz";
    private static final String SAML_NAME = "samlInstallationClient";

    private ClientResource oidcClient;
    private String oidcClientId;
    private ClientResource oidcBearerOnlyClient;
    private String oidcBearerOnlyClientId;
    private ClientResource oidcBearerOnlyClientWithAuthz;
    private String oidcBearerOnlyClientWithAuthzId;
    private ClientResource samlClient;
    private String samlClientId;

    @Before
    public void createClients() {
        oidcClientId = createOidcClient(OIDC_NAME);
        oidcBearerOnlyClientId = createOidcBearerOnlyClient(OIDC_NAME_BEARER_ONLY_NAME);

        oidcClient = findClientResource(OIDC_NAME);
        oidcBearerOnlyClient = findClientResource(OIDC_NAME_BEARER_ONLY_NAME);

        samlClientId = createSamlClient(SAML_NAME);
        samlClient = findClientResource(SAML_NAME);
    }

    @After
    public void tearDown() {
        removeClient(oidcClientId);
        removeClient(oidcBearerOnlyClientId);
        removeClient(samlClientId);
    }

    private String authServerUrl() {
        return AuthServerTestEnricher.getAuthServerContextRoot() + "/auth";
    }

    private String samlUrl() {
        return authServerUrl() + "/realms/test/protocol/saml";
    }

    @Test
    public void testOidcJBossXml() {
        String xml = oidcClient.getInstallationProvider("keycloak-oidc-jboss-subsystem");
        assertOidcInstallationConfig(xml);
        assertThat(xml, containsString("<secure-deployment"));
    }

    @Test
    public void testOidcJson() {
        String json = oidcClient.getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
    }

    @Test
    public void testOidcBearerOnlyJson() {
        String json = oidcBearerOnlyClient.getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
        assertThat(json, containsString("bearer-only"));
        assertThat(json, not(containsString("public-client")));
        assertThat(json, not(containsString("credentials")));
    }

    @Test
    public void testOidcBearerOnlyWithAuthzJson() {
        ProfileAssume.assumePreview();

        oidcBearerOnlyClientWithAuthzId = createOidcBearerOnlyClientWithAuthz(OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME);
        oidcBearerOnlyClientWithAuthz = findClientResource(OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME);

        String json = oidcBearerOnlyClientWithAuthz.getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
        assertThat(json, containsString("bearer-only"));
        assertThat(json, not(containsString("public-client")));
        assertThat(json, containsString("credentials"));
        assertThat(json, containsString("secret"));

        removeClient(oidcBearerOnlyClientWithAuthzId);
    }

    private void assertOidcInstallationConfig(String config) {
        assertThat(config, containsString("test"));
        assertThat(config, not(containsString(ApiUtil.findActiveKey(testRealmResource()).getPublicKey())));
        assertThat(config, containsString(authServerUrl()));
    }

    @Test
    public void testSamlMetadataIdpDescriptor() {
        String xml = samlClient.getInstallationProvider("saml-idp-descriptor");
        assertThat(xml, containsString("<EntityDescriptor"));
        assertThat(xml, containsString("<IDPSSODescriptor"));
        assertThat(xml, containsString(ApiUtil.findActiveKey(testRealmResource()).getCertificate()));
        assertThat(xml, containsString(samlUrl()));
    }

    @Test
    public void testSamlAdapterXml() {
        String xml = samlClient.getInstallationProvider("keycloak-saml");
        assertThat(xml, containsString("<keycloak-saml-adapter>"));
        assertThat(xml, containsString(SAML_NAME));
        assertThat(xml, not(containsString(ApiUtil.findActiveKey(testRealmResource()).getCertificate())));
        assertThat(xml, containsString(samlUrl()));
    }

    @Test
    public void testSamlMetadataSpDescriptor() {
        String xml = samlClient.getInstallationProvider("saml-sp-descriptor");
        assertThat(xml, containsString("<EntityDescriptor"));
        assertThat(xml, containsString("<SPSSODescriptor"));
        assertThat(xml, containsString(SAML_NAME));
    }

    @Test
    public void testSamlJBossXml() {
        String xml = samlClient.getInstallationProvider("keycloak-saml-subsystem");
        assertThat(xml, containsString("<secure-deployment"));
        assertThat(xml, containsString(SAML_NAME));
        assertThat(xml, not(containsString(ApiUtil.findActiveKey(testRealmResource()).getCertificate())));
        assertThat(xml, containsString(samlUrl()));
    }
}
