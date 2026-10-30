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
package org.keycloak.tests.saml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.utils.SecureContextResolver;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import static org.keycloak.saml.common.constants.GeneralConstants.RELAY_STATE;

/**
 * SAML client utility for test framework
 *
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
        public HttpUriRequest perform(CloseableHttpClient client, URI uri, CloseableHttpResponse response, HttpClientContext context) {
            return null;
        }
    }

    public static class RedirectStrategyWithSwitchableFollowRedirect extends LaxRedirectStrategy {
        private boolean redirectable = true;

        @Override
        protected boolean isRedirectable(String method) {
            return redirectable && super.isRedirectable(method);
        }

        public void setRedirectable(boolean redirectable) {
            this.redirectable = redirectable;
        }
    }

    /**
     * SAML bindings
     */
    public enum Binding {
        POST {
            @Override
            public HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY);
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlResponse) {
                return createSamlPostMessage(samlEndpoint, relayState, samlResponse, GeneralConstants.SAML_RESPONSE_KEY);
            }

            @Override
            public HttpUriRequest createSamlArtifactRequest(URI samlEndpoint, String relayState, String artifact) {
                HttpPost post = new HttpPost(samlEndpoint);
                List<NameValuePair> parameters = new LinkedList<>();
                parameters.add(new BasicNameValuePair(GeneralConstants.SAML_ARTIFACT_KEY, artifact));
                if (relayState != null) {
                    parameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
                }
                post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
                return post;
            }

            private HttpPost createSamlPostMessage(URI samlEndpoint, String relayState, Document samlDoc, String messageType) {
                HttpPost post = new HttpPost(samlEndpoint);
                List<NameValuePair> parameters = new LinkedList<>();

                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
                    parameters.add(
                            new BasicNameValuePair(messageType,
                                    binding
                                            .postBinding(samlDoc)
                                            .encoded())
                    );
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                if (relayState != null) {
                    parameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
                }

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
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
            public HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlRedirectMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY);
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlResponse) {
                return createSamlRedirectMessage(samlEndpoint, relayState, samlResponse, GeneralConstants.SAML_RESPONSE_KEY);
            }

            @Override
            public HttpUriRequest createSamlArtifactRequest(URI samlEndpoint, String relayState, String artifact) {
                try {
                    URIBuilder builder = new URIBuilder(samlEndpoint)
                            .setParameter(GeneralConstants.SAML_ARTIFACT_KEY, artifact);
                    if (relayState != null) {
                        builder.setParameter(RELAY_STATE, relayState);
                    }
                    return new HttpGet(builder.build());
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }

            private HttpGet createSamlRedirectMessage(URI samlEndpoint, String relayState, Document samlDoc, String messageType) {
                try {
                    String xml = DocumentUtil.getDocumentAsString(samlDoc);
                    String deflated = RedirectBindingUtil.deflateBase64Encode(xml.getBytes(GeneralConstants.SAML_CHARSET));
                    URIBuilder uriBuilder = new URIBuilder(samlEndpoint);
                    uriBuilder.addParameter(messageType, deflated);
                    if (relayState != null) {
                        uriBuilder.addParameter(RELAY_STATE, relayState);
                    }
                    return new HttpGet(uriBuilder.build());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();
            }
        };

        public abstract HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlResponse);

        public abstract HttpUriRequest createSamlArtifactRequest(URI samlEndpoint, String relayState, String artifact);

        public abstract URI getBindingUri();
    }

    private final HttpClientContext context = HttpClientContext.create();
    private final RedirectStrategyWithSwitchableFollowRedirect strategy = new RedirectStrategyWithSwitchableFollowRedirect();
    private ManagedCertificates certificates;

    public SamlClient() {
    }

    public SamlClient(ManagedCertificates certificates) {
        this.certificates = certificates;
    }

    public HttpClientContext getContext() {
        return context;
    }

    protected HttpClientBuilder createHttpClientBuilderInstance() {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .evictIdleConnections(100, TimeUnit.MILLISECONDS);

        CookieSpecProvider cookieSpec = ctx -> new DefaultCookieSpec() {
            @Override
            public boolean match(Cookie cookie, CookieOrigin origin) {
                if (SecureContextResolver.isLocal(origin.getHost())) {
                    origin = new CookieOrigin(origin.getHost(), origin.getPort(), origin.getPath(), true);
                }
                return super.match(cookie, origin);
            }
        };

        builder.setDefaultCookieSpecRegistry(RegistryBuilder.<CookieSpecProvider>create().register("browser", cookieSpec).build())
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec("browser").build());

        if (certificates != null && certificates.isTlsEnabled()) {
            builder.setSSLContext(certificates.getClientSSLContext());
        }

        return builder;
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
            for (int i = 0; i < steps.size(); i++) {
                Step s = steps.get(i);
                CloseableHttpResponse origResponse = currentResponse;

                HttpUriRequest request = s.perform(client, currentUri, origResponse, context);
                if (request == null) {
                    continue;
                }

                if (i < steps.size() - 1 && steps.get(i + 1) instanceof DoNotFollowRedirectStep) {
                    strategy.setRedirectable(false);
                    i++;
                } else {
                    strategy.setRedirectable(true);
                }

                context.removeAttribute(HttpClientContext.REDIRECT_LOCATIONS);
                currentResponse = client.execute(request, context);

                currentUri = request.getURI();
                List<URI> locations = context.getRedirectLocations();
                if (locations != null && !locations.isEmpty()) {
                    currentUri = locations.get(locations.size() - 1);
                }

                if (currentResponse != origResponse && origResponse != null) {
                    origResponse.close();
                }
            }

            if (currentResponse == null) {
                throw new IllegalStateException("No HTTP response was produced by the SAML client steps");
            }
            try (CloseableHttpResponse response = currentResponse) {
                return resultTransformer.extract(response);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates a SAML login request document
     */
    public static AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, URI destinationURI) {
        try {
            SAML2Request samlReq = new SAML2Request();
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(
                    UUID.randomUUID().toString(),
                    assertionConsumerURL,
                    destinationURI.toString(),
                    issuer
            );
            return loginReq;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
