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
import org.keycloak.testsuite.util.SamlClient.Step;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 *
 * @author hmlnarik
 */
public class UpdateProfileBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private final Map<String, String> parameters = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(UpdateProfileBuilder.class);

    public UpdateProfileBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        assertThat(currentResponse, statusCodeIsHC(Response.Status.OK));
        String loginPageText = EntityUtils.toString(currentResponse.getEntity(), "UTF-8");
        assertThat(loginPageText, containsString("Update Account Information"));

        return handleUpdateProfile(loginPageText, currentURI);
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public UpdateProfileBuilder param(String paramName, String paramValue) {
        if (paramValue != null) {
            this.parameters.put(paramName, paramValue);
        } else {
            this.parameters.remove(paramName);
        }
        return this;
    }

    public UpdateProfileBuilder firstName(String firstName) {
        return param("firstName", firstName);
    }

    public UpdateProfileBuilder lastName(String lastName) {
        return param("lastName", lastName);
    }

    public UpdateProfileBuilder username(String username) {
        return param("username", username);
    }

    public UpdateProfileBuilder email(String email) {
        return param("email", email);
    }

    public HttpUriRequest handleUpdateProfile(String loginPage, URI currentURI) {
        org.jsoup.nodes.Document theUpdateProfilePage = Jsoup.parse(loginPage);
        Set<String> unusedParams = new HashSet<>(this.parameters.keySet());

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theUpdateProfilePage.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            for (Element input : form.getElementsByTag("input")) {
                if (this.parameters.containsKey(input.attr("name"))) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), this.parameters.get(input.attr("name"))));
                    unusedParams.remove(input.attr("name"));
                }
            }

            if (! unusedParams.isEmpty()) {
                LOG.warnf("Unused parameter names at Update Profile page: %s", unusedParams);
            }

            if (isPost) {
                HttpPost res = new HttpPost(action);

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

        throw new IllegalArgumentException("Invalid update profile form: " + loginPage);
    }

}
