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

import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 *
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
        String consentPageText = EntityUtils.toString(currentResponse.getEntity(), "UTF-8");
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
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            for (Element input : form.getElementsByTag("input")) {
                if (Objects.equals(input.id(), "kc-login")) {
                    if (approveConsent)
                        parameters.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                } else if (Objects.equals(input.id(), "kc-cancel")) {
                    if (!approveConsent)
                        parameters.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                } else {
                    parameters.add(new BasicNameValuePair(input.attr("name"), input.val()));
                }
            }

            if (isPost) {
                HttpPost res = new HttpPost(currentURI.resolve(action));

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

}
