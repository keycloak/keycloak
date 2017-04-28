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
import org.keycloak.representations.idm.UserRepresentation;
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
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.http.protocol.HttpContext;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 * @author hmlnarik
 */
public class SamlClient {

    /**
     * SAML bindings and related HttpClient methods.
     */
    public enum Binding {
        POST {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException {
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
                    parameters.add(new BasicNameValuePair(GeneralConstants.RELAY_STATE, relayState));
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
                return URI.create(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
            }
        },

        REDIRECT {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.FOUND));
                String location = response.getFirstHeader("Location").getValue();
                response.close();
                return extractSamlResponseFromRedirect(location);
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
                return URI.create(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return null;
            }
        };

        public abstract SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException;

        public abstract HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey);

        public abstract URI getBindingUri();

        public abstract HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest);
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
     * Extracts and parses value of SAMLResponse query parameter from the given URI.
     *
     * @param responseUri
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromRedirect(String responseUri) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(responseUri), "UTF-8");

        String samlDoc = null;
        for (NameValuePair param : params) {
            if ("SAMLResponse".equals(param.getName()) || "SAMLRequest".equals(param.getName())) {
                assertThat("Only one SAMLRequest/SAMLResponse check", samlDoc, nullValue());
                samlDoc = param.getValue();
            }
        }

        return SAMLRequestParser.parseResponseRedirectBinding(samlDoc);
    }

    /**
     * Prepares a GET/POST request for logging the given user into the given login page. The login page is expected
     * to have at least input fields with id "username" and "password".
     *
     * @param user
     * @param loginPage
     * @return
     */
    public static HttpUriRequest handleLoginPage(UserRepresentation user, String loginPage) {
        String username = user.getUsername();
        String password = getPasswordOf(user);
        org.jsoup.nodes.Document theLoginPage = Jsoup.parse(loginPage);

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theLoginPage.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            for (Element input : form.getElementsByTag("input")) {
                if (Objects.equals(input.id(), "username")) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), username));
                } else if (Objects.equals(input.id(), "password")) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), password));
                } else {
                    parameters.add(new BasicNameValuePair(input.attr("name"), input.val()));
                }
            }

            if (isPost) {
                HttpPost res = new HttpPost(action);

                UrlEncodedFormEntity formEntity;
                try {
                    formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                res.setEntity(formEntity);

                return res;
            } else {
                UriBuilder b = UriBuilder.fromPath(action);
                for (NameValuePair parameter : parameters) {
                    b.queryParam(parameter.getName(), parameter.getValue());
                }
                return new HttpGet(b.build());
            }
        }

        throw new IllegalArgumentException("Invalid login form: " + loginPage);
    }

    /**
     * Prepares a GET/POST request for consent granting . The consent page is expected
     * to have at least input fields with id "kc-login" and "kc-cancel".
     *
     * @param consentPage
     * @param consent
     * @return
     */
    public static HttpUriRequest handleConsentPage(String consentPage, boolean consent) {
        org.jsoup.nodes.Document theLoginPage = Jsoup.parse(consentPage);

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theLoginPage.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            for (Element input : form.getElementsByTag("input")) {
                if (Objects.equals(input.id(), "kc-login")) {
                    if (consent)
                        parameters.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                } else if (Objects.equals(input.id(), "kc-cancel")) {
                    if (!consent)
                        parameters.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                } else {
                    parameters.add(new BasicNameValuePair(input.attr("name"), input.val()));
                }
            }

            if (isPost) {
                HttpPost res = new HttpPost(getAuthServerContextRoot() + action);

                UrlEncodedFormEntity formEntity;
                try {
                    formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                res.setEntity(formEntity);

                return res;
            } else {
                UriBuilder b = UriBuilder.fromPath(action);
                for (NameValuePair parameter : parameters) {
                    b.queryParam(parameter.getName(), parameter.getValue());
                }
                return new HttpGet(b.build());
            }
        }

        throw new IllegalArgumentException("Invalid consent page: " + consentPage);
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
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(UUID.randomUUID().toString(), assertionConsumerURL, destination.toString(), issuer);

            return loginReq;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Send request for login form and then login using user param. This method is designed for clients without required consent
     *
     * @param user
     * @param samlEndpoint
     * @param samlRequest
     * @param relayState
     * @param requestBinding
     * @param expectedResponseBinding
     * @return
     */
    public static SAMLDocumentHolder login(UserRepresentation user, URI samlEndpoint,
                                           Document samlRequest, String relayState, Binding requestBinding, Binding expectedResponseBinding) {
        return new SamlClient(samlEndpoint).login(user, samlRequest, relayState, requestBinding, expectedResponseBinding, false, true);
    }

    private final HttpClientContext context = HttpClientContext.create();
    private final URI samlEndpoint;

    public SamlClient(URI samlEndpoint) {
        this.samlEndpoint = samlEndpoint;
    }

    /**
     * Send request for login form and then login using user param. Check whether client requires consent and handle consent page.
     *
     * @param user
     * @param samlEndpoint
     * @param samlRequest
     * @param relayState
     * @param requestBinding
     * @param expectedResponseBinding
     * @param consentRequired
     * @param consent
     * @return
     */
    public SAMLDocumentHolder login(UserRepresentation user,
                                    Document samlRequest, String relayState, Binding requestBinding, Binding expectedResponseBinding, boolean consentRequired, boolean consent) {
        return getSamlResponse(expectedResponseBinding, (client, context, strategy) -> {
            HttpUriRequest post = requestBinding.createSamlUnsignedRequest(samlEndpoint, relayState, samlRequest);
            CloseableHttpResponse response = client.execute(post, context);

            assertThat(response, statusCodeIsHC(Response.Status.OK));
            String loginPageText = EntityUtils.toString(response.getEntity(), "UTF-8");
            response.close();

            assertThat(loginPageText, containsString("login"));

            HttpUriRequest loginRequest = handleLoginPage(user, loginPageText);

            if (consentRequired) {
                // Client requires consent
                response = client.execute(loginRequest, context);
                String consentPageText = EntityUtils.toString(response.getEntity(), "UTF-8");
                loginRequest = handleConsentPage(consentPageText, consent);
            }

            strategy.setRedirectable(false);
            return client.execute(loginRequest, context);
        });
    }

    /**
     * Send request for login form once already logged in, hence login using SSO.
     * Check whether client requires consent and handle consent page.
     *
     * @param user
     * @param samlEndpoint
     * @param samlRequest
     * @param relayState
     * @param requestBinding
     * @param expectedResponseBinding
     * @return
     */
    public SAMLDocumentHolder subsequentLoginViaSSO(Document samlRequest, String relayState, Binding requestBinding, Binding expectedResponseBinding) {
        return getSamlResponse(expectedResponseBinding, (client, context, strategy) -> {
            strategy.setRedirectable(false);

            HttpUriRequest post = requestBinding.createSamlUnsignedRequest(samlEndpoint, relayState, samlRequest);
            CloseableHttpResponse response = client.execute(post, context);
            assertThat(response, statusCodeIsHC(Response.Status.FOUND));
            String location = response.getFirstHeader("Location").getValue();

            response = client.execute(new HttpGet(location), context);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    /**
     * Send request for login form once already logged in, hence login using SSO.
     * Check whether client requires consent and handle consent page.
     *
     * @param user
     * @param samlEndpoint
     * @param samlRequest
     * @param relayState
     * @param requestBinding
     * @param expectedResponseBinding
     * @return
     */
    public SAMLDocumentHolder logout(Document samlRequest, String relayState, Binding requestBinding, Binding expectedResponseBinding) {
        return getSamlResponse(expectedResponseBinding, (client, context, strategy) -> {
            strategy.setRedirectable(false);

            HttpUriRequest post = requestBinding.createSamlUnsignedRequest(samlEndpoint, relayState, samlRequest);
            CloseableHttpResponse response = client.execute(post, context);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @FunctionalInterface
    public interface HttpClientProcessor {
        public CloseableHttpResponse process(CloseableHttpClient client, HttpContext context, RedirectStrategyWithSwitchableFollowRedirect strategy) throws Exception;
    }

    public void execute(HttpClientProcessor body) {
        CloseableHttpResponse response = null;
        RedirectStrategyWithSwitchableFollowRedirect strategy = new RedirectStrategyWithSwitchableFollowRedirect();

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(strategy).build()) {
            response = body.process(client, context, strategy);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try {
                    response.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public SAMLDocumentHolder getSamlResponse(Binding expectedResponseBinding, HttpClientProcessor body) {
        CloseableHttpResponse response = null;
        RedirectStrategyWithSwitchableFollowRedirect strategy = new RedirectStrategyWithSwitchableFollowRedirect();
        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(strategy).build()) {
            response = body.process(client, context, strategy);

            return expectedResponseBinding.extractResponse(response);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try {
                    response.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * Send request for login form and then login using user param for clients which doesn't require consent
     *
     * @param user
     * @param idpInitiatedURI
     * @param expectedResponseBinding
     * @return
     */
    public static SAMLDocumentHolder idpInitiatedLogin(UserRepresentation user, URI idpInitiatedURI, Binding expectedResponseBinding) {
        return new SamlClient(idpInitiatedURI).idpInitiatedLogin(user, expectedResponseBinding, false, true);
    }

    /**
     * Send request for login form and then login using user param. For clients which requires consent
     *
     * @param user
     * @param idpInitiatedURI
     * @param expectedResponseBinding
     * @param consent
     * @return
     */
    public static SAMLDocumentHolder idpInitiatedLoginWithRequiredConsent(UserRepresentation user, URI idpInitiatedURI, Binding expectedResponseBinding, boolean consent) {
        return new SamlClient(idpInitiatedURI).idpInitiatedLogin(user, expectedResponseBinding, true, consent);
    }

    /**
     * Send request for login form and then login using user param. Checks whether client requires consent and handle consent page.
     *
     * @param user
     * @param samlEndpoint
     * @param expectedResponseBinding
     * @param consent
     * @return
     */
    public SAMLDocumentHolder idpInitiatedLogin(UserRepresentation user, Binding expectedResponseBinding, boolean consentRequired, boolean consent) {
        return getSamlResponse(expectedResponseBinding, (client, context, strategy) -> {
            HttpGet get = new HttpGet(samlEndpoint);
            CloseableHttpResponse response = client.execute(get);
            assertThat(response, statusCodeIsHC(Response.Status.OK));

            String loginPageText = EntityUtils.toString(response.getEntity(), "UTF-8");
            response.close();

            assertThat(loginPageText, containsString("login"));

            HttpUriRequest loginRequest = handleLoginPage(user, loginPageText);

            if (consentRequired) {
                // Client requires consent
                response = client.execute(loginRequest, context);
                String consentPageText = EntityUtils.toString(response.getEntity(), "UTF-8");
                loginRequest = handleConsentPage(consentPageText, consent);
            }

            strategy.setRedirectable(false);
            return client.execute(loginRequest, context);
        });
    }


}
