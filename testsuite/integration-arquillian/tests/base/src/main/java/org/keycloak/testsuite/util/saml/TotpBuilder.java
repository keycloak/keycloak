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
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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
 *
 * @author hmlnarik
 */
public class TotpBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private String totpSecret;

    public TotpBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        assertThat(currentResponse, statusCodeIsHC(Response.Status.OK));
        String loginPageText = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);
        assertThat(loginPageText, containsString("login"));

        return handleOtpPage(loginPageText, currentURI);
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public TotpBuilder secret(String totpSecret) {
        this.totpSecret = totpSecret;
        return this;
    }

    /**
     * Prepares a POST request for logging the given user into the given login page. The login page is expected
     * to have at least input fields with id "username" and "password".
     *
     * @param loginPage
     * @return
     */
    private HttpUriRequest handleOtpPage(String loginPage, URI currentURI) {
        return handleOtpPage(totpSecret, loginPage);
    }

    public static HttpUriRequest handleOtpPage(String totpSecret, String loginPage) {

        org.jsoup.nodes.Document theLoginPage = Jsoup.parse(loginPage);

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theLoginPage.getElementsByTag("form")) {
            String action = form.attr("action");

            for (Element input : form.getElementsByTag("input")) {
                if (Objects.equals(input.id(), "otp")) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), new TimeBasedOTP().generateTOTP(totpSecret)));
                }
            }

            HttpPost res = new HttpPost(action);

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            res.setEntity(formEntity);

            return res;
        }

        throw new IllegalArgumentException("Invalid login form: " + loginPage);
    }

}
