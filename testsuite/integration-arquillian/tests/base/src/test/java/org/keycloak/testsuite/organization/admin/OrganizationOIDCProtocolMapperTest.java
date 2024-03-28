/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.AccessToken;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationOIDCProtocolMapperTest extends AbstractOrganizationTest {

    @Test
    public void testClaim() throws Exception {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization);

        oauth.clientId("direct-grant");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();

        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        @SuppressWarnings("unchecked")
        Map<String, Object> claim = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(claim, notNullValue());
        assertThat(claim.get(organizationName), notNullValue());
    }
}
