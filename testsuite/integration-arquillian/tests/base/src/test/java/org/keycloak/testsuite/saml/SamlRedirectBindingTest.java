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

import java.io.IOException;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_ID_SALES_POST;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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

    @Test
    public void testQueryParametersInSamlProcessingUriRedirectWithSignature() throws Exception {
        SamlClient samlClient = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG + "?param1=value1&param2=value2",
                        Binding.REDIRECT)
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build().doNotFollowRedirects()
                .execute(hr -> {
                    try {
                        // obtain the document validating the signature (it should be valid)
                        SAMLDocumentHolder doc = Binding.REDIRECT.extractResponse(hr, REALM_PUBLIC_KEY);
                        // assert doc is OK and the destination really has the extra parameters
                        assertThat(doc.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        assertThat(doc.getSamlObject(), instanceOf(ResponseType.class));
                        ResponseType res = (ResponseType) doc.getSamlObject();
                        assertThat(res.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG + "?param1=value1&param2=value2"));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }
}
