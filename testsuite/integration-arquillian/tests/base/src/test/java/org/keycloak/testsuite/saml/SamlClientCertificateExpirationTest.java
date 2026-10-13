/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.saml;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Test;

import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;

/**
 *
 * @author hmlnarik
 */
public class SamlClientCertificateExpirationTest extends AbstractSamlTest {

    @Test
    public void testExpiredCertificate() throws Exception {
        try (AutoCloseable cl = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST_SIG)
          .setAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE)
          .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
          .update()) {
            new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, Binding.POST)
                .signWith(SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY)
                .build()

              .assertResponse(Matchers.statusCodeIsHC(Status.BAD_REQUEST));
        }
    }

    @Test
    public void testValidCertificate() throws Exception {
        // Unsigned request should fail
        new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, Binding.POST)
            .build()
          .assertResponse(Matchers.statusCodeIsHC(Status.BAD_REQUEST));

        // Signed request should succeed
        new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, Binding.POST)
            .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
            .build()

          .assertResponse(Matchers.statusCodeIsHC(Status.OK));
    }
}
