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
package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.RealmBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class KcSamlParseIdPDescriptorTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("test").build());
    }

    @Test
    public void testIdPDescriptorParsing() throws IOException {
        // test default keycloak saml descriptor in the realm
        String descriptor = readSamlIdPDescriptor();
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("file", descriptor, MediaType.TEXT_XML_TYPE);
        Map<String, String> response = adminClient.realm("test").identityProviders().importFrom(output);
        Assert.assertNotNull(response.get(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY));
        Assert.assertEquals(authServerPage.toString() + "/realms/test", response.get(SAMLIdentityProviderConfig.IDP_ENTITY_ID));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.POST_BINDING_LOGOUT));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED));
        Assert.assertEquals(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(), response.get("nameIDPolicyFormat"));
        Assert.assertEquals(authServerPage.toString() + "/realms/test/protocol/saml", response.get(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL));
        Assert.assertEquals(authServerPage.toString() + "/realms/test/protocol/saml", response.get(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL));
        Assert.assertEquals(authServerPage.toString() + "/realms/test/protocol/saml/resolve", response.get(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL));

        // modify it with WantAuthnRequestsSigned=false
        descriptor = descriptor.replaceFirst("WantAuthnRequestsSigned=\"true\"", "WantAuthnRequestsSigned=\"false\"");
        output = new MultipartFormDataOutput();
        output.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("file", descriptor, MediaType.TEXT_XML_TYPE);
        response = adminClient.realm("test").identityProviders().importFrom(output);
        Assert.assertEquals("false", response.get(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED));
        Assert.assertEquals("true", response.get(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
    }

    private String readSamlIdPDescriptor() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(authServerPage.toString() + "/realms/test/protocol/saml/descriptor");
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

}
