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
package org.keycloak.testsuite.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.rotation.KeyLocator;
import static org.keycloak.saml.common.constants.GeneralConstants.RELAY_STATE;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 * @author hmlnarik
 */
public class SamlClient {

    @FunctionalInterface
    public interface Step {
        HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception;
    }

    @FunctionalInterface
    public interface ResultExtractor<T> {
        T extract(CloseableHttpResponse response) throws Exception;
    }

    public static final class DoNotFollowRedirectStep implements Step {

        @Override
        public HttpUriRequest perform(CloseableHttpClient client, URI uri, CloseableHttpResponse response, HttpClientContext context) throws Exception {
            return null;
        }
    }

    public static class RedirectStrategyWithSwitchableFollowRedirect extends LaxRedirectStrategy {

        public boolean redirectable = true;

        @Override
        protected boolean isRedirectable(String method) {
            return redirectable && super.isRedirectable(method);
        }

        public void setRedirectable(boolean redirectable) {
            this.redirectable = redirectable;
        }
    }

    /**
     * SAML bindings and related HttpClient methods.
     */
    public enum Binding {
        POST {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.OK));
                String responsePage = EntityUtils.toString(response.getEntity(), "UTF-8");
                response.close();
                return extractSamlResponseFromForm(responsePage);
            }

            @Override
            public HttpPost createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY, null, null);
            }

            @Override
            public HttpPost createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_RESPONSE_KEY, null, null);
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return null;
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.OK));
                String responsePage = EntityUtils.toString(response.getEntity(), "UTF-8");
                response.close();
                return extractSamlRelayStateFromForm(responsePage);
            }

            @Override
            public HttpPost createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY, realmPrivateKey, realmPublicKey);
            }

            private HttpPost createSamlPostMessage(URI samlEndpoint, String relayState, Document samlRequest, String messageType, String privateKeyStr, String publicKeyStr) {
                HttpPost post = new HttpPost(samlEndpoint);

                List<NameValuePair> parameters = new LinkedList<>();


                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

                    if (privateKeyStr != null && publicKeyStr != null) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKeyStr);
                        PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKeyStr);
                        binding
                                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey)
                                .signDocument();
                    }

                    parameters.add(
                            new BasicNameValuePair(messageType,
                                    binding
                                            .postBinding(samlRequest)
                                            .encoded())
                    );
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                if (relayState != null) {
                    parameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
                }

                UrlEncodedFormEntity formEntity;

                try {
                    formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                post.setEntity(formEntity);

                return post;
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            }
        },

        REDIRECT {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.FOUND));
                String location = response.getFirstHeader("Location").getValue();
                response.close();
                return extractSamlResponseFromRedirect(location, realmPublicKey);
            }

            @Override
            public HttpGet createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                try {
                    URI requestURI = new BaseSAML2BindingBuilder()
                            .relayState(relayState)
                            .redirectBinding(samlRequest)
                            .requestURI(samlEndpoint.toString());
                    return new HttpGet(requestURI);
                } catch (ProcessingException | ConfigurationException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                try {
                    URI responseURI = new BaseSAML2BindingBuilder()
                            .relayState(relayState)
                            .redirectBinding(samlRequest)
                            .responseURI(samlEndpoint.toString());
                    return new HttpGet(responseURI);
                } catch (ProcessingException | ConfigurationException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {

                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

                    if (realmPrivateKey != null && realmPublicKey != null) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(realmPrivateKey);
                        PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(realmPublicKey);
                        binding
                                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey)
                                .signDocument();
                    }

                    binding.relayState(relayState);

                    return new HttpGet(binding.redirectBinding(samlRequest).responseURI(samlEndpoint.toString()));
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.FOUND));
                String location = response.getFirstHeader("Location").getValue();
                response.close();
                return extractRelayStateFromRedirect(location);
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String privateKeyStr, String publicKeyStr) {
                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder().relayState(relayState);
                    if (privateKeyStr != null && publicKeyStr != null) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKeyStr);
                        PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKeyStr);
                        binding.signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey)
                                .signDocument();
                    }
                    return new HttpGet(binding.redirectBinding(samlRequest).requestURI(samlEndpoint.toString()));
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        public abstract SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException;

        public SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException {
            return extractResponse(response, null);
        }

        public abstract HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey);

        public abstract URI getBindingUri();

        public abstract HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey);

        public abstract String extractRelayState(CloseableHttpResponse response) throws IOException;
    }

    private static final Logger LOG = Logger.getLogger(SamlClient.class);

    private final HttpClientContext context = HttpClientContext.create();

    private final RedirectStrategyWithSwitchableFollowRedirect strategy = new RedirectStrategyWithSwitchableFollowRedirect();

    /**
     * Extracts and parses value of SAMLResponse input field of a form present in the given page.
     *
     * @param responsePage HTML code of the page
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromForm(String responsePage) {
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements samlResponses = theResponsePage.select("input[name=SAMLResponse]");
        Elements samlRequests = theResponsePage.select("input[name=SAMLRequest]");
        int size = samlResponses.size() + samlRequests.size();
        assertThat("Checking uniqueness of SAMLResponse/SAMLRequest input field in the page", size, is(1));

        Element respElement = samlResponses.isEmpty() ? samlRequests.first() : samlResponses.first();

        return SAMLRequestParser.parseResponsePostBinding(respElement.val());
    }

    /**
     * Extracts and parses value of RelayState input field of a form present in the given page.
     *
     * @param responsePage HTML code of the page
     * @return
     */
    public static String extractSamlRelayStateFromForm(String responsePage) {
        assertThat(responsePage, containsString("form name=\"saml-post-binding\""));
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements samlRelayStates = theResponsePage.select("input[name=RelayState]");

        if (samlRelayStates.isEmpty()) return null;

        return samlRelayStates.first().val();
    }

    /**
     * Extracts and parses value of RelayState query parameter from the given URI.
     *
     * @param responseUri
     * @return
     */
    public static String extractRelayStateFromRedirect(String responseUri) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(responseUri), "UTF-8");

        return params.stream().filter(nameValuePair -> nameValuePair.getName().equals(RELAY_STATE))
                .findFirst().map(NameValuePair::getValue).orElse(null);
    }

    public static MultivaluedMap<String, String> parseEncodedQueryParameters(String queryString) throws IOException {
        MultivaluedMap<String, String> encodedParams = new MultivaluedHashMap<>();
        if (queryString != null) {
            String[] params = queryString.split("&");
            for (String param : params) {
                if (param.indexOf('=') >= 0) {
                    String[] nv = param.split("=", 2);
                    encodedParams.add(RedirectBindingUtil.urlDecode(nv[0]), nv.length > 1 ? nv[1] : "");
                } else {
                    encodedParams.add(RedirectBindingUtil.urlDecode(param), "");
                }
            }
        }
        return encodedParams;
    }

    /**
     * Extracts and parses value of SAMLResponse query parameter from the given URI.
     * If the realmPublicKey parameter is passed the response signature is
     * validated.
     *
     * @param responseUri The redirect URI to use
     * @param realmPublicKey The public realm key for validating signature in REDIRECT query parameters
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromRedirect(String responseUri, String realmPublicKey) throws IOException {
        MultivaluedMap<String, String> encodedParams = parseEncodedQueryParameters(URI.create(responseUri).getRawQuery());

        String samlResponse = encodedParams.getFirst(GeneralConstants.SAML_RESPONSE_KEY);
        String samlRequest = encodedParams.getFirst(GeneralConstants.SAML_REQUEST_KEY);
        assertTrue("Only one SAMLRequest/SAMLResponse check", (samlResponse != null && samlRequest == null)
                || (samlResponse == null && samlRequest != null));

        String samlDoc = RedirectBindingUtil.urlDecode(samlResponse != null? samlResponse : samlRequest);
        SAMLDocumentHolder documentHolder = SAMLRequestParser.parseResponseRedirectBinding(samlDoc);

        if (realmPublicKey != null) {
            // if the public key is passed verify the signature of the redirect URI
            try {
                KeyLocator locator = new KeyLocator() {
                    @Override
                    public Key getKey(String kid) throws KeyManagementException {
                        return org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(realmPublicKey);
                    }

                    @Override
                    public void refreshKeyCache() {
                    }
                };
                SamlProtocolUtils.verifyRedirectSignature(documentHolder, locator, encodedParams,
                        samlResponse != null? GeneralConstants.SAML_RESPONSE_KEY : GeneralConstants.SAML_REQUEST_KEY);
            } catch (VerificationException e) {
                throw new IOException(e);
            }
        }

        return documentHolder;
    }

    /**
     * Creates a SAML login request document with the given parameters. See SAML &lt;AuthnRequest&gt; description for more details.
     *
     * @param issuer
     * @param assertionConsumerURL
     * @param destination
     * @return
     */
    public static AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, URI destination) {
        try {
            SAML2Request samlReq = new SAML2Request();
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(UUID.randomUUID().toString(), assertionConsumerURL,
              destination == null ? null : destination.toString(), issuer);

            return loginReq;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void execute(Step... steps) {
        executeAndTransform(resp -> null, Arrays.asList(steps));
    }

    public void execute(List<Step> steps) {
        executeAndTransform(resp -> null, steps);
    }

    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer, Step... steps) {
        return executeAndTransform(resultTransformer, Arrays.asList(steps));
    }

    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer, List<Step> steps) {
        CloseableHttpResponse currentResponse = null;
        URI currentUri = URI.create("about:blank");
        strategy.setRedirectable(true);

        try (CloseableHttpClient client = createHttpClientBuilderInstance().setRedirectStrategy(strategy).build()) {
            for (int i = 0; i < steps.size(); i ++) {
                Step s = steps.get(i);
                LOG.infof("Running step %d: %s", i, s.getClass());

                CloseableHttpResponse origResponse = currentResponse;

                HttpUriRequest request = s.perform(client, currentUri, origResponse, context);
                if (request == null) {
                    LOG.info("Last step returned no request, continuing with next step.");
                    continue;
                }

                // Setting of follow redirects has to be set before executing the final request of the current step
                if (i < steps.size() - 1 && steps.get(i + 1) instanceof DoNotFollowRedirectStep) {
                    LOG.debugf("Disabling following redirects");
                    strategy.setRedirectable(false);
                    i++;
                } else {
                    strategy.setRedirectable(true);
                }

                LOG.infof("Executing HTTP request to %s", request.getURI());
                currentResponse = client.execute(request, context);

                currentUri = request.getURI();
                List<URI> locations = context.getRedirectLocations();
                if (locations != null && ! locations.isEmpty()) {
                    currentUri = locations.get(locations.size() - 1);
                }

                LOG.infof("Landed to %s", currentUri);

                if (currentResponse != origResponse && origResponse != null) {
                    origResponse.close();
                }
            }

            LOG.info("Going to extract response");

            return resultTransformer.extract(currentResponse);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public HttpClientContext getContext() {
        return context;
    }

    protected HttpClientBuilder createHttpClientBuilderInstance() {
        return HttpClientBuilder.create();
    }
}