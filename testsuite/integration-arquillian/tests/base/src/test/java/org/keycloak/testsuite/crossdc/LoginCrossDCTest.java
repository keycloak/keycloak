/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.crossdc;

import javax.ws.rs.core.Response;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginCrossDCTest extends AbstractAdminCrossDCTest {

    @Test
    public void loginTest() throws Exception {
        enableDcOnLoadBalancer(DC.SECOND);

        //log.info("Started to sleep");
        //Thread.sleep(10000000);
        for (int i=0 ; i<30 ; i++) {
            OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
            String code = response1.getCode();
            OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(code, "password");
            Assert.assertNotNull(response2.getAccessToken());

            try (CloseableHttpResponse response3 = oauth.doLogout(response2.getRefreshToken(), "password")) {
                assertThat(response3, Matchers.statusCodeIsHC(Response.Status.NO_CONTENT));
                //assertNotNull(testingClient.testApp().getAdminLogoutAction());
            }

            log.infof("Iteration %d finished", i);
        }
    }
}
