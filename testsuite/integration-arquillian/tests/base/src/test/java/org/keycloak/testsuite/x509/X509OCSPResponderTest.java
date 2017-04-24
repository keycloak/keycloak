/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;

/**
 * Verifies Certificate revocation using OCSP responder.
 * The tests rely on an OCSP responder service listening
 * for OCSP requests on http://localhost:8888
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 11/2/2016
 */

public class X509OCSPResponderTest extends AbstractX509AuthenticationTest {

    private static final String OCSP_RESPONDER_HOST = "localhost";

    private static final int OCSP_RESPONDER_PORT = 8888;

    private Undertow ocspResponder;

    @Test
    public void loginFailedOnOCSPResponderRevocationCheck() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setOCSPEnabled(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
        assertEquals("invalid_request", response.getError());

        Assert.assertThat(response.getErrorDescription(), containsString("Certificate's been revoked."));
    }

    @Before
    public void startOCSPResponder() throws Exception {
        ocspResponder = Undertow.builder().addHttpListener(OCSP_RESPONDER_PORT, OCSP_RESPONDER_HOST)
                .setHandler(new BlockingHandler(new OcspHandler())).build();

        ocspResponder.start();
    }

    @After
    public void stopOCSPResponder() {
        ocspResponder.stop();
    }

}
