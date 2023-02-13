/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.rest.representation;

import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelRequest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TestAuthenticationChannelRequest {

    private String bearerToken;
    private AuthenticationChannelRequest request;

    public TestAuthenticationChannelRequest() {
        // for reflection
    }

    public TestAuthenticationChannelRequest(AuthenticationChannelRequest request, String bearerToken) {
        setBearerToken(bearerToken);
        setRequest(request);
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setRequest(AuthenticationChannelRequest request) {
        this.request = request;
    }

    public AuthenticationChannelRequest getRequest() {
        return request;
    }
}
