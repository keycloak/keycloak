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
import java.net.URI;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author hmlnarik
 */
public class IdPInitiatedLoginBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private final URI authServerSamlUrl;
    private final String clientId;

    public IdPInitiatedLoginBuilder(URI authServerSamlUrl, String clientId, SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        this.authServerSamlUrl = authServerSamlUrl;
        this.clientId = clientId;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        return new HttpGet(authServerSamlUrl.toString() + "/clients/" + this.clientId);
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }
}
