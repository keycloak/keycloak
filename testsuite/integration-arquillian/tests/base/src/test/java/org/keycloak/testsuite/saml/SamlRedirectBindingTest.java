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

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_ID_SALES_POST;

/**
 *
 * @author hmlnarik
 */
public class SamlRedirectBindingTest extends AbstractSamlTest {

    @Test
    public void testNoWhitespaceInLoginRequest() throws Exception {
        AuthnRequestType authnRequest = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, getAuthServerSamlEndpoint(REALM_NAME));
        HttpUriRequest req = SamlClient.Binding.REDIRECT.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, SAML2Request.convert(authnRequest));
        String url = req.getURI().getQuery();

        assertThat(url, not(containsString(" ")));
        assertThat(url, not(containsString("\n")));
        assertThat(url, not(containsString("\r")));
        assertThat(url, not(containsString("\t")));
    }
}
