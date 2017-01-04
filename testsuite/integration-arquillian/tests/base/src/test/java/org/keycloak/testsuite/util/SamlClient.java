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

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.w3c.dom.Document;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.Matchers.*;

/**
 *
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
            public HttpPost createSamlRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                HttpPost post = new HttpPost(samlEndpoint);

                List<NameValuePair> parameters = new LinkedList<>();
                try {
                    parameters.add(
                      new BasicNameValuePair(GeneralConstants.SAML_REQUEST_KEY,
                      new BaseSAML2BindingBuilder()
                        .postBinding(samlRequest)
                        .encoded())
                    );
                } catch (ProcessingException | ConfigurationException | IOException ex) {
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
            public HttpGet createSamlRequest(URI samlEndpoint, String relayState, Document samlRequest) {
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
        };

        public abstract SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException;
        public abstract HttpUriRequest createSamlRequest(URI samlEndpoint, String relayState, Document samlRequest);
        public abstract URI getBindingUri();
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
     * @param responsePage HTML code of the page
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromForm(String responsePage) {
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements samlResponses = theResponsePage.select("input[name=SAMLResponse]");
        assertThat("Checking uniqueness of SAMLResponse input field in the page", samlResponses, hasSize(1));

        Element respElement = samlResponses.first();

        return SAMLRequestParser.parseResponsePostBinding(respElement.val());
    }

    /**
     * Extracts and parses value of SAMLResponse query parameter from the given URI.
     * @param responseUri
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromRedirect(String responseUri) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(responseUri), "UTF-8");

        String samlResponse = null;
        for (NameValuePair param : params) {
            if ("SAMLResponse".equals(param.getName())) {
                assertThat(samlResponse, nullValue());
                samlResponse = param.getValue();
            }
        }

        return SAMLRequestParser.parseResponseRedirectBinding(samlResponse);
    }

    /**
     * Prepares a GET/POST request for logging the given user into the given login page. The login page is expected
     * to have at least input fields with id "username" and "password".
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
     * Creates a SAML login request document with the given parameters. See SAML &lt;AuthnRequest&gt; description for more details.
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

}
