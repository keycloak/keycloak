/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.util.HashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.ReverseProxy;
import org.keycloak.testsuite.util.SamlClient;
import org.w3c.dom.Document;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 * SAML tests using a {@code frontendUrl} that points to a reverse proxy. The SAML request destination should be validated
 * against the proxy address and any redirection should also have the proxy as target.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class SamlReverseProxyTest extends AbstractSamlTest {

    @ClassRule
    public static ReverseProxy proxy = new ReverseProxy();

    /**
     * KEYCLOAK-12612
     *
     * Tests sending a SAML {@code AuthnRequest} through a reverse proxy. In this scenario the SAML {@code AuthnRequest}
     * has a destination that matches the proxy server, but the request is forwarded to a keycloak server running in a
     * different address.
     *
     * Validation of the destination and subsequent redirection to the login screen only work if the proxy server is configured
     * as the {@code frontendUrl} of the realm.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testAuthnRequestWithReverseProxy() throws Exception {
        // send an authn request without defining the frontendUrl for the realm - should get a BAD_REQUEST response
        Document document = SAML2Request.convert(SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST,
                SAML_ASSERTION_CONSUMER_URL_SALES_POST, this.buildSamlProtocolUrl(proxy.getUrl())));
        testSendSamlRequest(document, Response.Status.BAD_REQUEST, containsString("Invalid Request"));

        // set the frontendUrl pointing to the reverse proxy
        RealmRepresentation rep = adminClient.realm(REALM_NAME).toRepresentation();
        try {
            if (rep.getAttributes() == null) {
                rep.setAttributes(new HashMap<>());
            }
            rep.getAttributes().put("frontendUrl", proxy.getUrl());
            adminClient.realm(REALM_NAME).update(rep);

            // resend the authn request - should succeed this time
            testSendSamlRequest(document, Response.Status.OK, containsString("login"));
        } finally {
            // restore the state of the realm (unset the frontendUrl)
            rep.getAttributes().remove("frontendUrl");
            adminClient.realm(REALM_NAME).update(rep);
        }
    }

    /**
     * KEYCLOAK-12944
     *
     * Tests sending a SAML {@code LogoutRequest} through a reverse proxy. In this scenario the SAML {@code LogoutRequest}
     * has a destination that matches the proxy server, but the request is forwarded to a keycloak server running in a
     * different address.
     *
     * Validation of the destination and any subsequent redirection only work if the proxy server is configured as the
     * {@code frontendUrl} of the realm.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testLogoutRequestWithReverseProxy() throws Exception {
        // send a logout request without defining the frontendUrl for the realm - should get a BAD_REQUEST response
        Document document = new SAML2LogoutRequestBuilder().destination(
                this.buildSamlProtocolUrl(proxy.getUrl()).toString()).issuer(SAML_CLIENT_ID_SALES_POST).buildDocument();
        testSendSamlRequest(document, Response.Status.BAD_REQUEST, containsString("Invalid Request"));

        // set the frontendUrl pointing to the reverse proxy
        RealmRepresentation rep = adminClient.realm(REALM_NAME).toRepresentation();
        try {
            if (rep.getAttributes() == null) {
                rep.setAttributes(new HashMap<>());
            }
            rep.getAttributes().put("frontendUrl", proxy.getUrl());
            adminClient.realm(REALM_NAME).update(rep);

            // resend the logout request - should succeed this time (we are actually not logging out anyone, just checking the request is properly validated
            testSendSamlRequest(document, Response.Status.OK, containsString("login"));
        } finally {
            // restore the state of the realm (unset the frontendUrl)
            rep.getAttributes().remove("frontendUrl");
            adminClient.realm(REALM_NAME).update(rep);
        }
    }

    private void testSendSamlRequest(final Document doc, final Response.Status expectedHttpCode, final Matcher<String> pageTextMatcher) throws Exception {
        HttpUriRequest post =
                SamlClient.Binding.POST.createSamlUnsignedRequest(this.buildSamlProtocolUrl(proxy.getUrl()), null, doc);
        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier((s, sslSession) -> true).
                setRedirectStrategy(new SamlClient.RedirectStrategyWithSwitchableFollowRedirect()).build();
             CloseableHttpResponse response = client.execute(post)) {
            assertThat(response, statusCodeIsHC(expectedHttpCode));
            assertThat(EntityUtils.toString(response.getEntity(), "UTF-8"), pageTextMatcher);
        }
    }

    private URI buildSamlProtocolUrl(final String baseUri) {
        return RealmsResource.protocolUrl(UriBuilder.fromUri(baseUri)).build(REALM_NAME, SamlProtocol.LOGIN_PROTOCOL);
    }

}
