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

import org.junit.Test;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import static org.junit.Assert.assertTrue;

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
            AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
            String code = response1.getCode();
            AccessTokenResponse response2 = oauth.doAccessTokenRequest(code);
            Assert.assertNotNull(response2.getAccessToken());

            LogoutResponse logoutResponse = oauth.doLogout(response2.getRefreshToken());
            assertTrue(logoutResponse.isSuccess());

            log.infof("Iteration %d finished", i);
        }
    }
}
