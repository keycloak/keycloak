/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testframework.realm.ManagedRealm;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;

import static org.keycloak.tests.saml.SamlClient.createLoginRequestDocument;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SamlBrowserlessBrokerClient implements AutoCloseable {

    private final CloseableHttpClient http;
    private final HttpClientContext context;
    private final WebClient webClient;
    private final ManagedRealm realm;
    private final String brokerAlias;
    private String clientId;

    public SamlBrowserlessBrokerClient(ManagedRealm realm, String clientId, String brokerAlias) {
        this.realm = realm;
        this.clientId = clientId;
        this.brokerAlias = brokerAlias;
        this.http = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultCookieStore(new BasicCookieStore())
                .disableContentCompression()
                .addInterceptorFirst(createSecureCookieRewriteInterceptor())
                .build();
        this.context = HttpClientContext.create();
        this.context.setCookieStore(new BasicCookieStore());
        this.webClient = new WebClient();
        this.webClient.getOptions().setJavaScriptEnabled(false);
        this.webClient.getOptions().setCssEnabled(false);
    }

    public IdentityProviderResponse login(String username, String password) {
        String consumerBaseUrl = realm.getBaseUrl();
        String consumerServerRoot = consumerBaseUrl.substring(0, consumerBaseUrl.indexOf("/realms/"));
        String assertionConsumerUrl = consumerServerRoot + "/sales-post/saml";
        URI samlEndpoint = URI.create(consumerBaseUrl + "/protocol/saml");
        AuthnRequestType authnRequest = createLoginRequestDocument(clientId, assertionConsumerUrl, samlEndpoint);
        String consumerLoginPage;

        try {
            consumerLoginPage = body(SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, SAML2Request.convert(authnRequest)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String providerLoginPage = sendAuthnRequest(consumerLoginPage, brokerAlias);
        String samlResponseFormPage = submitLoginForm(providerLoginPage, username, password);

        return new IdentityProviderResponse(readSamlResponseForm(samlResponseFormPage));
    }

    private HttpResponseInterceptor createSecureCookieRewriteInterceptor() {
        return (HttpResponse response, org.apache.http.protocol.HttpContext ctx) -> {
            Header[] setCookies = response.getHeaders("Set-Cookie");
            if (setCookies.length == 0) {
                return;
            }
            response.removeHeaders("Set-Cookie");
            for (Header header : setCookies) {
                response.addHeader("Set-Cookie", header.getValue().replaceAll("(?i);\\s*Secure", ""));
            }
        };
    }

    private String sendAuthnRequest(String consumerLoginPage, String brokerAlias) {
        String ssoServiceLocation = findBrokerLoginHref(consumerLoginPage, brokerAlias);
        HtmlForm form = getHtmlForm(body(new HttpGet(ssoServiceLocation)));
        List<NameValuePair> params = new LinkedList<>();

        for (HtmlInput input : form.<HtmlInput>getByXPath(".//input[@type='hidden']")) {
            params.add(new BasicNameValuePair(input.getAttribute("name"), input.getValueAttribute()));
        }

        return body(formPost(form.getActionAttribute(), params));
    }

    private String submitLoginForm(String page, String username, String password) {
        HtmlForm form = (HtmlForm) parse(page).getElementById("kc-form-login");
        if (form == null) {
            throw new IllegalStateException("Login form (form#kc-form-login) was not found on the page");
        }
        List<NameValuePair> params = new LinkedList<>();
        for (HtmlInput input : form.<HtmlInput>getByXPath(".//input")) {
            String id = input.getAttribute("id");
            String name = input.getAttribute("name");
            if ("username".equals(id)) {
                params.add(new BasicNameValuePair(name, username));
            } else if ("password".equals(id)) {
                params.add(new BasicNameValuePair(name, password));
            } else if (!name.isEmpty()) {
                params.add(new BasicNameValuePair(name, input.getValueAttribute()));
            }
        }
        return body(formPost(form.getActionAttribute(), params));
    }

    private SamlPostForm readSamlResponseForm(String page) {
        HtmlForm form = getHtmlForm(page);
        String samlResponse = hiddenValue(form, "SAMLResponse");
        if (samlResponse == null) {
            throw new IllegalStateException("Page does not contain a SAMLResponse form");
        }
        return new SamlPostForm(form.getActionAttribute(), samlResponse, hiddenValue(form, "RelayState"));
    }

    private int postSamlResponse(SamlPostForm form, String samlResponse) {
        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("SAMLResponse", samlResponse));
        if (form.relayState() != null) {
            params.add(new BasicNameValuePair("RelayState", form.relayState()));
        }
        try (CloseableHttpResponse response = http.execute(formPost(form.action(), params), context)) {
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a GET request to {@code url} in this client's browser session, following redirects, and returns
     * the final status code. The URL is used as is, so percent-encoded characters in it are not re-encoded.
     */
    public int get(String url) {
        try (CloseableHttpResponse response = http.execute(new HttpGet(url), context)) {
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String findBrokerLoginHref(String page, String brokerAlias) {
        HtmlAnchor anchor = parse(page).getFirstByXPath("//a[contains(@href, 'broker/" + brokerAlias + "/login')]");
        String href = anchor == null ? null : anchor.getAttribute("href");

        assertNotNull(href, "Consumer login page must expose the SAML broker login link");

        if (href.startsWith("http")) {
            return href;
        }

        String baseUrl = realm.getBaseUrl();
        String root = baseUrl.substring(0, baseUrl.indexOf("/realms/"));

        return href.startsWith("/") ? root + href : root + "/" + href;
    }

    @Override
    public void close() throws IOException {
        webClient.close();
        http.close();
    }

    private HtmlForm getHtmlForm(String page) {
        List<HtmlForm> forms = parse(page).getForms();

        if (forms.isEmpty()) {
            throw new IllegalStateException("No form was found on the page");
        }

        return forms.get(0);
    }

    private HtmlPage parse(String page) {
        try {
            return webClient.loadHtmlCodeIntoCurrentWindow(page);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hiddenValue(HtmlForm form, String name) {
        List<HtmlInput> inputs = form.getInputsByName(name);
        return inputs.isEmpty() ? null : inputs.get(0).getValueAttribute();
    }

    private static HttpPost formPost(String url, List<NameValuePair> params) {
        HttpPost post = new HttpPost(url);
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return post;
    }

    private String body(HttpUriRequest request) {
        try (CloseableHttpResponse response = http.execute(request, context)) {
            return response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final class IdentityProviderResponse {

        private final SamlPostForm form;
        private final SamlResponseDocument document;

        private IdentityProviderResponse(SamlPostForm form) {
            this.form = form;
            this.document = form.responseDocument();
        }

        public IdentityProviderResponse onResponse(Consumer<SamlResponseDocument> mutation) {
            mutation.accept(document);
            return this;
        }

        public int submit() {
            return postSamlResponse(form, document.toPostBinding());
        }

        public SamlResponseDocument document() {
            return document;
        }

        public String relayState() {
            return form.relayState();
        }

        /**
         * Delivers the response to the broker endpoint with the HTTP-Redirect binding, sending {@code rawQuery}
         * as the query string verbatim, and returns the final status code.
         */
        public int submitRedirect(String rawQuery) {
            return get(form.action() + "?" + rawQuery);
        }
    }

    private record SamlPostForm(String action, String samlResponse, String relayState) {

        private SamlResponseDocument responseDocument() {
            return SamlResponseDocument.fromPostBinding(samlResponse);
        }
    }
}
