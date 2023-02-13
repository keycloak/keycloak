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

package org.keycloak.testsuite.util;

import org.junit.Assert;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.UserInfo;
import org.keycloak.utils.MediaType;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserInfoClientUtil {

    public static Response executeUserInfoRequest_getMethod(Client client, String accessToken) {
        WebTarget userInfoTarget = getUserInfoWebTarget(client);

        return userInfoTarget.request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .get();
    }

    public static WebTarget getUserInfoWebTarget(Client client) {
        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        UriBuilder uriBuilder = OIDCLoginProtocolService.userInfoUrl(builder);
        URI userInfoUri = uriBuilder.build("test");
        return client.target(userInfoUri);
    }

    public static UserInfo testSuccessfulUserInfoResponse(Response response, String expectedUsername, String expectedEmail) {
        return testSuccessfulUserInfoResponse(response, null, expectedUsername, expectedEmail);
    }

    public static UserInfo testSuccessfulUserInfoResponse(Response response, String userId, String expectedUsername, String expectedEmail) {
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JSON);

        UserInfo userInfo = response.readEntity(UserInfo.class);

        response.close();

        Assert.assertNotNull(userInfo);
        Assert.assertNotNull(userInfo.getSubject());
        if (userId != null) {
            Assert.assertEquals(userId, userInfo.getSubject());
        }
        Assert.assertEquals(expectedEmail, userInfo.getEmail());
        Assert.assertEquals(expectedUsername, userInfo.getPreferredUsername());
        return userInfo;
    }

}
