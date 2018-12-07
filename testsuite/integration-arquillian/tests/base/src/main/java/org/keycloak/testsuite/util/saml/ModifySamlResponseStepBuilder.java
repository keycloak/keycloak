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

import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testsuite.util.SamlClient.Binding;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.core.Response.Status;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;


public class ModifySamlResponseStepBuilder extends SamlDocumentStepBuilder<SAML2Object, ModifySamlResponseStepBuilder> {

    private final Binding binding;

    private URI targetUri;
    private String targetAttribute;
    private Binding targetBinding;

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
        }

        throw new RuntimeException("Unknown binding for " + ModifySamlResponseStepBuilder.class.getName());
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

    public URI targetUri() {
        return targetUri;
    }

    public ModifySamlResponseStepBuilder targetUri(URI forceUri) {
        this.targetUri = forceUri;
        return this;
    }

    protected HttpUriRequest handleRedirectBinding(CloseableHttpResponse currentResponse) throws Exception, IOException, URISyntaxException {
        NameValuePair samlParam = null;

        assertThat(currentResponse, statusCodeIsHC(Status.FOUND));
        String location = currentResponse.getFirstHeader("Location").getValue();
        URI locationUri = URI.create(location);

        List<NameValuePair> params = URLEncodedUtils.parse(locationUri, "UTF-8");
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
        String samlDoc = IOUtils.toString(decoded, GeneralConstants.SAML_CHARSET);
        IOUtils.closeQuietly(decoded);

        String transformed = getTransformer().transform(samlDoc);
        if (transformed == null) {
            return null;
        }

        final String attrName = this.targetAttribute != null ? this.targetAttribute : samlParam.getName();

        return createRequest(locationUri, attrName, transformed, params);
    }

    private HttpUriRequest handlePostBinding(CloseableHttpResponse currentResponse) throws Exception {
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
        String samlDoc = IOUtils.toString(decoded, GeneralConstants.SAML_CHARSET);
        IOUtils.closeQuietly(decoded);

        String transformed = getTransformer().transform(samlDoc);
        if (transformed == null) {
            return null;
        }

        final String attributeName = this.targetAttribute != null
          ? this.targetAttribute
          : respElement.attr("name");
        List<NameValuePair> parameters = new LinkedList<>();

        if (! relayStates.isEmpty()) {
            parameters.add(new BasicNameValuePair(GeneralConstants.RELAY_STATE, relayStates.first().val()));
        }
        URI locationUri = this.targetUri != null
          ? this.targetUri
          : URI.create(form.attr("action"));

        return createRequest(locationUri, attributeName, transformed, parameters);
    }

    protected HttpUriRequest createRequest(URI locationUri, String attributeName, String transformed, List<NameValuePair> parameters) throws IOException, URISyntaxException {
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
