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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.saml.LoginBuilder;
import org.keycloak.testsuite.utils.io.IOUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.keycloak.testsuite.util.SamlClient.Binding;
import static org.keycloak.testsuite.util.SamlClient.RedirectStrategyWithSwitchableFollowRedirect;
import static org.keycloak.testsuite.util.SamlClient.createLoginRequestDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author hmlnarik
 */
@Ignore
public class ConcurrentAuthnRequestTest extends AbstractSamlTest {

    public static final int ITERATIONS = 10000;
    public static final int CONCURRENT_THREADS = 5;

    private void loginRepeatedly(UserRepresentation user, URI samlEndpoint,
      String relayState, Binding requestBinding) {
        CloseableHttpResponse response = null;
        SamlClient.RedirectStrategyWithSwitchableFollowRedirect strategy = new SamlClient.RedirectStrategyWithSwitchableFollowRedirect();
        ExecutorService threadPool = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(strategy).build()) {
            
            Collection<Callable<Void>> futures = new LinkedList<>();
            for (int i = 0; i < ITERATIONS; i ++) {
                final int j = i;
                AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, REALM_NAME);
                Document samlRequest = SAML2Request.convert(loginRep);
                HttpUriRequest post = requestBinding.createSamlUnsignedRequest(samlEndpoint, relayState, samlRequest);
                Callable<Void> f = () -> {
                    performLogin(post, samlEndpoint, relayState, loginRep.getID(), samlRequest, response, client, user, strategy);
                    return null;
                };
                futures.add(f);
            }

            threadPool.invokeAll(futures);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void performLogin(HttpUriRequest post, URI samlEndpoint, String relayState,
      String requestId, Document samlRequest, CloseableHttpResponse response, final CloseableHttpClient client,
      UserRepresentation user,
      RedirectStrategyWithSwitchableFollowRedirect strategy) {
        try {
            HttpClientContext context = HttpClientContext.create();
            response = client.execute(post, context);

            String loginPageText = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            response.close();

            HttpUriRequest loginRequest = LoginBuilder.handleLoginPage(user, loginPageText);

            strategy.setRedirectable(false);
            response = client.execute(loginRequest, context);
            SAMLDocumentHolder parseResponsePostBinding = SAMLRequestParser.parseResponsePostBinding(EntityUtils.toString(response.getEntity()));
            assertThat(parseResponsePostBinding.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            assertThat(((ResponseType) parseResponsePostBinding.getSamlObject()).getInResponseTo(), is(requestId));
            response.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try { response.close(); } catch (IOException ex) { }
            }
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Override
    public AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, String realmName) {
        return SamlClient.createLoginRequestDocument(issuer, assertionConsumerURL, getAuthServerSamlEndpoint(realmName));
    }

    private void testLogin(Binding requestBinding) throws Exception {
        loginRepeatedly(bburkeUser, getAuthServerSamlEndpoint(REALM_NAME), null, requestBinding);
    }

    @Test
    public void testConcurrentPostLogins() throws Exception {
        testLogin(Binding.POST);
    }
}
