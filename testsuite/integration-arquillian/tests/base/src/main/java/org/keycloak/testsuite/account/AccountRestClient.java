/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.account.CredentialMetadataRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.TokenUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Helper client for account REST API
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountRestClient implements AutoCloseable {

    private final SuiteContext suiteContext;
    private final CloseableHttpClient httpClient;
    private final Supplier<String> tokenProvider;
    private final String apiVersion;
    private final String realmName;

    private AccountRestClient(SuiteContext suiteContext, CloseableHttpClient httpClient, Supplier<String> tokenProvider, String apiVersion, String realmName) {
        this.suiteContext = suiteContext;
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.apiVersion = apiVersion;
        this.realmName = realmName;
    }

    public List<AccountCredentialResource.CredentialContainer> getCredentials() {
        try {
            return SimpleHttpDefault.doGet(getAccountUrl("credentials"), httpClient)
                    .auth(tokenProvider.get()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {
                    });
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to get credentials", ioe);
        }
    }

    public CredentialRepresentation getCredentialByUserLabel(String userLabel) {
        return getCredentials().stream()
                .flatMap(credentialContainer -> credentialContainer.getUserCredentialMetadatas().stream())
                .map(CredentialMetadataRepresentation::getCredential)
                .filter(credentialRep -> userLabel.equals(credentialRep.getUserLabel()))
                .findFirst()
                .orElse(null);
    }

    public SimpleHttpResponse removeCredential(String credentialId) {
        try {
            return SimpleHttpDefault
                    .doDelete(getAccountUrl("credentials/" + credentialId), httpClient)
                    .acceptJson()
                    .auth(tokenProvider.get())
                    .asResponse();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to delete credential", ioe);
        }
    }

    // TODO: Other objects...


    @Override
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException ioe) {
                throw new RuntimeException("Error closing httpClient", ioe);
            }
        }
    }


    private String getAccountUrl(String resource) {
        String url = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realmName + "/account";
        if (apiVersion != null) {
            url += "/" + apiVersion;
        }
        if (resource != null) {
            url += "/" + resource;
        }
        return url;
    }

    public static AccountRestClientBuilder builder(SuiteContext suiteContext) {
        return new AccountRestClientBuilder(suiteContext);
    }


    public static class AccountRestClientBuilder {

        private SuiteContext suiteContext;
        private CloseableHttpClient httpClient;
        private Supplier<String> tokenProvider;
        private String apiVersion;
        private String realmName;

        private AccountRestClientBuilder(SuiteContext suiteContext) {
            this.suiteContext = suiteContext;
        }

        public AccountRestClientBuilder httpClient(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public AccountRestClientBuilder tokenUtil(TokenUtil tokenUtil) {
            this.tokenProvider = tokenUtil::getToken;
            return this;
        }

        public AccountRestClientBuilder accessToken(String accessToken) {
            this.tokenProvider = () -> accessToken;
            return this;
        }

        public AccountRestClientBuilder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public AccountRestClientBuilder realmName(String realmName) {
            this.realmName = realmName;
            return this;
        }

        public AccountRestClient build() {
            if (httpClient == null) {
                httpClient = HttpClientBuilder.create().build();
            }
            if (realmName == null) {
                realmName = "test";
            }
            if (tokenProvider == null) {
                TokenUtil tokenUtil = new TokenUtil();
                tokenProvider = tokenUtil::getToken;
            }
            return new AccountRestClient(suiteContext, httpClient, tokenProvider, apiVersion, realmName);
        }

    }

}
