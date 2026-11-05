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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LoginBuilder implements SamlClient.Step {

    private final SamlClientBuilder clientBuilder;
    private String username;
    private String password;
    private String idpAlias;

    public LoginBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public LoginBuilder user(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public LoginBuilder idp(String idpAlias) {
        this.idpAlias = idpAlias;
        return this;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        assertEquals(HttpStatus.SC_OK, currentResponse.getStatusLine().getStatusCode());
        String loginPageText = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);

        if (idpAlias != null) {
            org.jsoup.nodes.Document doc = Jsoup.parse(loginPageText);
            Element socialLink = doc.getElementById("social-" + this.idpAlias);
            assertNotNull(socialLink, "Unknown idp: " + this.idpAlias);
            String link = socialLink.attr("href");
            assertNotNull(link, "Invalid idp link: " + this.idpAlias);
            return new HttpGet(currentURI.resolve(link));
        }

        return handleLoginPage(loginPageText, currentURI);
    }

    private HttpUriRequest handleLoginPage(String loginPage, URI currentURI) {
        org.jsoup.nodes.Document doc = Jsoup.parse(loginPage);
        for (Element form : doc.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            List<NameValuePair> parameters = new LinkedList<>();
            for (Element input : form.getElementsByTag("input")) {
                String id = input.id();
                String name = input.attr("name");
                if ("username".equalsIgnoreCase(id) || "username".equalsIgnoreCase(name)) {
                    if (name.isEmpty()) {
                        throw new IllegalStateException("Username input has no name attribute");
                    }
                    parameters.add(new BasicNameValuePair(name, username));
                } else if ("password".equalsIgnoreCase(id) || "password".equalsIgnoreCase(name)) {
                    if (name.isEmpty()) {
                        throw new IllegalStateException("Password input has no name attribute");
                    }
                    parameters.add(new BasicNameValuePair(name, password));
                } else if (name != null && !name.isEmpty()) {
                    parameters.add(new BasicNameValuePair(name, input.val()));
                }
            }

            URI targetUri = currentURI.resolve(action);
            if (isPost) {
                HttpPost post = new HttpPost(targetUri);
                post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
                return post;
            } else {
                URIBuilder b = new URIBuilder(targetUri);
                for (NameValuePair parameter : parameters) {
                    b.addParameter(parameter.getName(), parameter.getValue());
                }
                try {
                    return new HttpGet(b.build());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException("Invalid login form: " + loginPage);
    }
}
