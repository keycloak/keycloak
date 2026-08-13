/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.util.EntityUtils;
import org.junit.Test;

import static org.keycloak.testsuite.util.SamlClient.Binding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 *
 * @author hmlnarik
 */
public class AuthnRequestTest extends AbstractSamlTest {

    // KEYCLOAK-7316
    @Test
    public void testIsPassiveNotSet() throws Exception {
        String res = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
            .transformObject(so -> {
                so.setIsPassive(null);
                return so;
            })
            .build()

          .executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

        assertThat(res, containsString("login"));
    }

    // KEYCLOAK-7316
    @Test
    public void testIsForceAuthNotSet() throws Exception {
        String res = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
                .transformObject(so -> {
                    so.setForceAuthn(null);
                    return so;
                })
                .build()

                .executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

        assertThat(res, containsString("login"));
    }

    // KEYCLOAK-7316
    @Test
    public void testIsPassiveFalse() throws Exception {
        String res = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
            .transformObject(so -> {
                so.setIsPassive(false);
                return so;
            })
            .build()

          .executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

        assertThat(res, containsString("login"));
    }

    // KEYCLOAK-7331
    @Test
    public void testIssuerNotSet() throws Exception {
        String res = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
            .transformObject(so -> {
                so.setIssuer(null);
                return so;
            })
            .build()

          .executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

        assertThat(res, containsString("login"));
    }

}
