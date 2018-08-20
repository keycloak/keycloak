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

package org.keycloak.testsuite.admin.group;

import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;

import java.security.PublicKey;
import java.util.List;

import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGroupTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    AccessToken login(String login, String clientId, String clientSecret, String userId) throws Exception {
        AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("test", login, "password", null, clientId, clientSecret);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        PublicKey publicKey = PemUtils.decodePublicKey(ApiUtil.findActiveKey(adminClient.realm("test")).getPublicKey());

        AccessToken accessTokenRepresentation = RSATokenVerifier.verifyToken(accessToken, publicKey, AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/realms/test");

        JWSInput jws = new JWSInput(refreshToken);
        RefreshToken refreshTokenRepresentation = jws.readJsonContent(RefreshToken.class);

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessTokenRepresentation.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshTokenRepresentation.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        return accessTokenRepresentation;
    }

    RealmRepresentation loadTestRealm(List<RealmRepresentation> testRealms) {
        RealmRepresentation result = loadRealm("/testrealm.json");
        testRealms.add(result);
        return result;
    }
}
