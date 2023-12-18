/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.client;

import org.junit.Test;

public class OAuth2_1PublicClientTest extends AbstractFAPITest {

    @Test
    public void testOAuth2_1NotAllowImplicitGrant() throws Exception {
        // TODO
        // token request by implicit grant
    }

    @Test
    public void testOAuth2_1NotAllowResourceOwnerPasswordCredentialsGrant() throws Exception {
        // TODO
        // token request by resource owner password credentials grant - fail
    }

    @Test
    public void testOAuth2_1ProofKeyForCodeExchange() throws Exception {
        // TODO
        // registration (auto-config) - success
        // update (auto-config) - success
        // authorization request - fail
        // authorization request - success, token request - fail
        // authorization request - success, token request - success
    }

    @Test
    public void testOAuth2_1RedirectUris() throws Exception {
        // TODO
        // registration with invalid redirect_uri - fail
        // registration with valid redirect_uri- success
        // update with invalid redirect_uri - fail
        // update with valid redirect_uri - success
        // authorization with invalid redirect_uri request - fail
        // authorization with valid redirect_uri request - success
    }

    @Test
    public void testOAuth2_1DPoPSenderConstrainedToken() throws Exception {
        // TODO
        // registration (auto-config) - success
        // update (auto-config) with disabling DPoP - success
        // authorization request - success
        // token request without DPoP Proof - fail
        // token request with DPoP Proof - success
        // token refresh request with DPoP Proof by other key - fail
        // token refresh request with DPoP Proof by the same key - success
        // userinfo request without DPoP Proof - fail
        // userinfo request with DPoP Proof by the same key - success
        // token revocation without DPoP Proof - fail
        // token revocation with DPoP Proof by the same key - success
    }

}