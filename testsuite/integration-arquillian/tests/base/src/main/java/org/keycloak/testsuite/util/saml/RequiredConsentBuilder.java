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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author hmlnarik
 */
public class RequiredConsentBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private boolean approveConsent = true;

    public RequiredConsentBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        assertThat(currentResponse, statusCodeIsHC(Response.Status.OK));
        String consentPageText = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);
        assertThat(consentPageText, containsString("consent"));
        assertThat(consentPageText, containsString("My Roles")); // Corresponding to role_list default SAML client scope

        return handleConsentPage(consentPageText, currentURI);
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public RequiredConsentBuilder approveConsent(boolean shouldApproveConsent) {
        this.approveConsent = shouldApproveConsent;
        return this;
    }

    /**
     * Prepares a GET/POST request for consent granting . The consent page is expected
     * to have at least input fields with id "kc-login" and "kc-cancel".
     *
     * @param consentPage
     * @param consent
     * @return
     */
    public HttpUriRequest handleConsentPage(String consentPage, URI currentURI) {
        org.jsoup.nodes.Document theLoginPage = Jsoup.parse(consentPage);

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theLoginPage.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = "post".equalsIgnoreCase(method);

            Element submitButton;
            if (approveConsent) {
                submitButton = form.getElementById("kc-login");
            } else {
                submitButton = form.getElementById("kc-cancel");
            }
            parameters.add(new BasicNameValuePair(submitButton.attr("name"), submitButton.attr("value")));

            for (Element input : form.getElementsByTag("input")) {
                parameters.add(new BasicNameValuePair(input.attr("name"), input.val()));
            }

            if (isPost) {
                HttpPost res = new HttpPost(currentURI.resolve(action));

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
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

}
