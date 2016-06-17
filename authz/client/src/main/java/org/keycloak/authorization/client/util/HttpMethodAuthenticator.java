/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.client.util;

import org.keycloak.OAuth2Constants;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpMethodAuthenticator<R> {

    private final HttpMethod<R> method;

    public HttpMethodAuthenticator(HttpMethod<R> method) {
        this.method = method;
    }

    public HttpMethod<R> oauth2ClientCredentials() {
        this.method.params.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS);
        configureClientCredentials();
        return this.method;
    }

    public HttpMethod<R> oauth2ResourceOwnerPassword(String userName, String password) {
        this.method.params.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        this.method.params.put("username", userName);
        this.method.params.put("password", password);
        configureClientCredentials();
        return this.method;
    }

    private void configureClientCredentials() {
        this.method.configuration.getClientAuthenticator().configureClientCredentials(this.method.params, this.method.headers);
    }
}
