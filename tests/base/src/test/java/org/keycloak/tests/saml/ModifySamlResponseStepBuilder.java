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

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import org.apache.http.HttpStatus;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModifySamlResponseStepBuilder implements SamlClient.Step {

    @FunctionalInterface
    public interface Saml2DocumentTransformer {
        Document transform(Document document);
    }

    private final SamlClient.Binding binding;
    private final SamlClientBuilder clientBuilder;
    private String targetAttribute;
    private Saml2DocumentTransformer transformer = doc -> doc;

    public ModifySamlResponseStepBuilder(SamlClient.Binding binding, SamlClientBuilder clientBuilder) {
        this.binding = binding;
        this.clientBuilder = clientBuilder;
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public ModifySamlResponseStepBuilder targetAttribute(String attribute) {
        this.targetAttribute = attribute;
        return this;
    }

    public ModifySamlResponseStepBuilder targetAttributeSamlRequest() {
        return targetAttribute(GeneralConstants.SAML_REQUEST_KEY);
    }

    public ModifySamlResponseStepBuilder targetAttributeSamlResponse() {
        return targetAttribute(GeneralConstants.SAML_RESPONSE_KEY);
    }

    public ModifySamlResponseStepBuilder transformDocument(Consumer<Document> tr) {
        return transformDocument(doc -> {
            tr.accept(doc);
            return doc;
        });
    }

    public ModifySamlResponseStepBuilder transformDocument(Saml2DocumentTransformer tr) {
        Saml2DocumentTransformer prev = this.transformer;
        this.transformer = doc -> {
            Document intermediate = prev.transform(doc);
            return intermediate == null ? null : tr.transform(intermediate);
        };
        return this;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        if (binding == SamlClient.Binding.POST) {
            return handlePostBinding(currentURI, currentResponse);
        } else if (binding == SamlClient.Binding.REDIRECT) {
            return handleRedirectBinding(currentURI, currentResponse);
        }
        throw new IllegalArgumentException("Unsupported binding: " + binding);
    }

    private HttpUriRequest handlePostBinding(URI currentURI, CloseableHttpResponse currentResponse) throws Exception {
        assertEquals(HttpStatus.SC_OK, currentResponse.getStatusLine().getStatusCode());
        String htmlBody = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);

        org.jsoup.nodes.Document page = Jsoup.parse(htmlBody);
        Elements samlResponses = page.select("input[name=SAMLResponse]");
        Elements samlRequests = page.select("input[name=SAMLRequest]");
        Elements forms = page.select("form");

        assertFalse(forms.isEmpty(), "Expected at least one form in page: " + htmlBody);
        Element form = forms.first();
        Element respElement = !samlResponses.isEmpty() ? samlResponses.first() : samlRequests.first();
        assertNotNull(respElement, "Missing SAMLResponse or SAMLRequest input in page: " + htmlBody);

        String samlDocXml = new String(Base64.getMimeDecoder().decode(respElement.val()), StandardCharsets.UTF_8);
        Document document = DocumentUtil.getDocument(samlDocXml);
        Document transformed = transformer.transform(document);
        assertNotNull(transformed, "SAML document transformer returned null");

        String modifiedSaml = DocumentUtil.getDocumentAsString(transformed);
        String base64Modified = Base64.getEncoder().encodeToString(modifiedSaml.getBytes(StandardCharsets.UTF_8));

        String respElementName = respElement.attr("name");
        String attrName = targetAttribute != null ? targetAttribute : respElementName;
        URI actionUri = currentURI.resolve(form.attr("action"));

        List<NameValuePair> params = new LinkedList<>();
        for (Element input : form.select("input")) {
            String name = input.attr("name");
            if (name == null || name.isEmpty() || name.equals(respElementName) || name.equals(attrName)) {
                continue;
            }
            params.add(new BasicNameValuePair(name, input.val()));
        }
        params.add(new BasicNameValuePair(attrName, base64Modified));

        HttpPost post = new HttpPost(actionUri);
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return post;
    }

    private HttpUriRequest handleRedirectBinding(URI currentURI, CloseableHttpResponse currentResponse) throws Exception {
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, currentResponse.getStatusLine().getStatusCode());
        String location = currentResponse.getFirstHeader("Location").getValue();
        URI locationUri = currentURI.resolve(location);

        List<NameValuePair> params = URLEncodedUtils.parse(locationUri, StandardCharsets.UTF_8);
        NameValuePair samlParam = null;
        for (Iterator<NameValuePair> it = params.iterator(); it.hasNext();) {
            NameValuePair param = it.next();
            if (GeneralConstants.SAML_RESPONSE_KEY.equals(param.getName()) || GeneralConstants.SAML_REQUEST_KEY.equals(param.getName())) {
                samlParam = param;
                it.remove();
            }
        }
        assertNotNull(samlParam, "Missing SAML parameter in redirect location: " + location);

        String samlDocXml;
        try (InputStream decoded = RedirectBindingUtil.base64DeflateDecode(samlParam.getValue())) {
            samlDocXml = new String(decoded.readAllBytes(), GeneralConstants.SAML_CHARSET);
        }

        Document document = DocumentUtil.getDocument(samlDocXml);
        Document transformed = transformer.transform(document);
        String modifiedSaml = DocumentUtil.getDocumentAsString(transformed);

        String deflated = RedirectBindingUtil.deflateBase64Encode(modifiedSaml.getBytes(GeneralConstants.SAML_CHARSET));
        String attrName = targetAttribute != null ? targetAttribute : samlParam.getName();
        params.add(new BasicNameValuePair(attrName, deflated));

        URI target = new URIBuilder(locationUri).setParameters(params).build();
        return new HttpGet(target);
    }
}
