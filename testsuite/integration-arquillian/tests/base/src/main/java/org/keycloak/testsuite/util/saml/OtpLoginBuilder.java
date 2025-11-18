/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

/**
 *
 * @author rmartinc
 */
public class OtpLoginBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private String otpPassword;

    public OtpLoginBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public OtpLoginBuilder otp(String otpPassword) {
        this.otpPassword = otpPassword;
        return this;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        assertThat(currentResponse, statusCodeIsHC(Response.Status.OK));
        String otpPageText = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);
        return handleOtpLoginPage(otpPageText, otpPassword);
    }

    public static HttpUriRequest handleOtpLoginPage(String loginPage, String otpPassword) {
        org.jsoup.nodes.Document page = Jsoup.parse(loginPage);
        Element form = page.getElementById("kc-otp-login-form");
        if (form == null) {
            throw new IllegalArgumentException("Invalid OTP login form: " + loginPage);
        }

        String action = form.attr("action");
        if (action == null) {
            throw new IllegalArgumentException("Invalid OTP login form: " + loginPage);
        }

        HttpPost res = new HttpPost(action);
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("otp", otpPassword));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        res.setEntity(formEntity);

        return res;
    }
}
