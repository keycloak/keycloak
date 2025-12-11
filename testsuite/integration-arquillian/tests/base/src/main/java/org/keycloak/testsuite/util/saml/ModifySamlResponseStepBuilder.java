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
package org.keycloak.testsuite.util.saml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;


public class ModifySamlResponseStepBuilder extends SamlDocumentStepBuilder<SAML2Object, ModifySamlResponseStepBuilder> {

    private final Binding binding;

    private URI targetUri;
    private String targetAttribute;
    private Binding targetBinding;
    private Supplier<String> documentSupplier;

    public ModifySamlResponseStepBuilder(Binding binding, SamlClientBuilder clientBuilder) {
        super(clientBuilder);
        this.binding = binding;
        this.targetBinding = binding;
    }

    // TODO: support for signing
    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        switch (binding) {
            case REDIRECT:
                return handleRedirectBinding(currentResponse);

            case POST:
                return handlePostBinding(currentResponse);
                
            case ARTIFACT_RESPONSE:
                return handleArtifactResponse(currentResponse);
        }

        throw new RuntimeException("Unknown binding for " + ModifySamlResponseStepBuilder.class.getName());
    }

    public Supplier<String> documentSupplier() {
        return documentSupplier;
    }

    public ModifySamlResponseStepBuilder documentSupplier(Supplier<String> documentSupplier) {
        this.documentSupplier = documentSupplier;
        return this;
    }

    public Binding targetBinding() {
        return targetBinding;
    }

    public ModifySamlResponseStepBuilder targetBinding(Binding targetBinding) {
        this.targetBinding = targetBinding;
        return this;
    }

    public String targetAttribute() {
        return targetAttribute;
    }

    public ModifySamlResponseStepBuilder targetAttribute(String attribute) {
        targetAttribute = attribute;
        return this;
    }

    public ModifySamlResponseStepBuilder targetAttributeSamlRequest() {
        return targetAttribute(GeneralConstants.SAML_REQUEST_KEY);
    }

    public ModifySamlResponseStepBuilder targetAttributeSamlResponse() {
        return targetAttribute(GeneralConstants.SAML_RESPONSE_KEY);
    }

    public ModifySamlResponseStepBuilder targetAttributeSamlArtifact() {
        return targetAttribute(GeneralConstants.SAML_ARTIFACT_KEY);
    }

    public URI targetUri() {
        return targetUri;
    }

    public ModifySamlResponseStepBuilder targetUri(URI forceUri) {
        this.targetUri = forceUri;
        return this;
    }

    private HttpUriRequest handleArtifactResponse(CloseableHttpResponse currentResponse) throws Exception {
        SAMLDocumentHolder samlDocumentHolder = null;
        try {
            samlDocumentHolder = Binding.ARTIFACT_RESPONSE.extractResponse(currentResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return createRequest(this.targetUri, this.targetAttribute, DocumentUtil.asString(samlDocumentHolder.getSamlDocument()), new LinkedList<>());
    }

    protected HttpUriRequest handleRedirectBinding(CloseableHttpResponse currentResponse) throws Exception, IOException, URISyntaxException {
        String samlDoc;
        final String attrName;
        final URI uri;
        final List<NameValuePair> params;

        if (documentSupplier != null) {
            Objects.requireNonNull(this.targetUri, "Set targetUri");
            Objects.requireNonNull(this.targetAttribute, "Set targetAttribute");

            samlDoc = documentSupplier.get();
            uri = this.targetUri;
            attrName = this.targetAttribute;
            params = new LinkedList<>();
        } else {
            NameValuePair samlParam = null;

            assertThat(currentResponse, statusCodeIsHC(Status.FOUND));
            String location = currentResponse.getFirstHeader("Location").getValue();
            URI locationUri = URI.create(location);

            params = URLEncodedUtils.parse(locationUri, StandardCharsets.UTF_8);
            for (Iterator<NameValuePair> it = params.iterator(); it.hasNext();) {
                NameValuePair param = it.next();
                if ("SAMLResponse".equals(param.getName()) || "SAMLRequest".equals(param.getName())) {
                    assertThat("Only one SAMLRequest/SAMLResponse check", samlParam, nullValue());
                    samlParam = param;
                    it.remove();
                }
            }

            assertThat(samlParam, notNullValue());

            String base64EncodedSamlDoc = samlParam.getValue();
            InputStream decoded = RedirectBindingUtil.base64DeflateDecode(base64EncodedSamlDoc);
            samlDoc = IOUtils.toString(decoded, GeneralConstants.SAML_CHARSET);
            IOUtils.closeQuietly(decoded);
            
            uri = this.targetUri != null
               ? this.targetUri
               : locationUri;
            attrName = this.targetAttribute != null ? this.targetAttribute : samlParam.getName();
        }

        return createRequest(uri, attrName, samlDoc, params);
    }

    private HttpUriRequest handlePostBinding(CloseableHttpResponse currentResponse) throws Exception {
        String samlDoc;
        final String attrName;
        final URI uri;
        final List<NameValuePair> params = new LinkedList<>();

        if (documentSupplier != null) {
            Objects.requireNonNull(this.targetUri, "Set targetUri");
            Objects.requireNonNull(this.targetAttribute, "Set targetAttribute");

            samlDoc = documentSupplier.get();
            uri = this.targetUri;
            attrName = this.targetAttribute;
        } else {
            assertThat(currentResponse, statusCodeIsHC(Status.OK));

            final String htmlBody = EntityUtils.toString(currentResponse.getEntity());
            assertThat(htmlBody, Matchers.containsString("SAML"));
            org.jsoup.nodes.Document theResponsePage = Jsoup.parse(htmlBody);
            Elements samlResponses = theResponsePage.select("input[name=SAMLResponse]");
            Elements samlRequests = theResponsePage.select("input[name=SAMLRequest]");
            Elements forms = theResponsePage.select("form");
            Elements relayStates = theResponsePage.select("input[name=RelayState]");
            int size = samlResponses.size() + samlRequests.size();
            assertThat("Checking uniqueness of SAMLResponse/SAMLRequest input field in the page", size, is(1));
            assertThat("Checking uniqueness of forms in the page", forms, hasSize(1));

            Element respElement = samlResponses.isEmpty() ? samlRequests.first() : samlResponses.first();
            Element form = forms.first();

            String base64EncodedSamlDoc = respElement.val();
            InputStream decoded = PostBindingUtil.base64DecodeAsStream(base64EncodedSamlDoc);
            samlDoc = IOUtils.toString(decoded, GeneralConstants.SAML_CHARSET);
            IOUtils.closeQuietly(decoded);

            attrName = this.targetAttribute != null
              ? this.targetAttribute
              : respElement.attr("name");

            if (! relayStates.isEmpty()) {
                params.add(new BasicNameValuePair(GeneralConstants.RELAY_STATE, relayStates.first().val()));
            }
            uri = this.targetUri != null
              ? this.targetUri
              : URI.create(form.attr("action"));
        }

        return createRequest(uri, attrName, samlDoc, params);
    }

    protected HttpUriRequest createRequest(URI locationUri, String attributeName, String samlDoc, List<NameValuePair> parameters) throws Exception {
        String transformed = getTransformer().transform(samlDoc);
        if (transformed == null) {
            return null;
        }

        switch (this.targetBinding) {
            case POST:
                return createPostRequest(locationUri, attributeName, transformed, parameters);
            case REDIRECT:
                return createRedirectRequest(locationUri, attributeName, transformed, parameters);
        }
        throw new RuntimeException("Unknown target binding for " + ModifySamlResponseStepBuilder.class.getName());
    }

    protected HttpUriRequest createRedirectRequest(URI locationUri, String attributeName, String transformed, List<NameValuePair> parameters) throws IOException, URISyntaxException {
        final byte[] responseBytes = transformed.getBytes(GeneralConstants.SAML_CHARSET);
        parameters.add(new BasicNameValuePair(attributeName, RedirectBindingUtil.deflateBase64Encode(responseBytes)));

        if (this.targetUri != null) {
            locationUri = this.targetUri;
        }

        URI target = new URIBuilder(locationUri).setParameters(parameters).build();

        return new HttpGet(target);
    }

    protected HttpUriRequest createPostRequest(URI locationUri, String attributeName, String transformed, List<NameValuePair> parameters) throws IOException {
        HttpPost post = new HttpPost(locationUri);

        parameters.add(new BasicNameValuePair(attributeName, PostBindingUtil.base64Encode(transformed)));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, GeneralConstants.SAML_CHARSET);
        post.setEntity(formEntity);

        return post;
    }
}
