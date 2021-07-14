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

package org.keycloak.testsuite.oauth;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createAddressMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createClaimMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedRole;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createScriptMapper;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.UriUtils;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCProtocolMappersUserProfileTest extends OIDCProtocolMappersTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        final RealmRepresentation testRealm = testRealms.get(0);
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }

    @Before
    public void onBefore() {
        VerifyProfileTest.setUserProfileConfiguration(adminClient.realm("test"), "{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"group-value\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"street\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"departments\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"locality\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"region_some\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"postal_code\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"country\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"formatted\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"phone\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"json-attribute\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"json-attribute-multi\"," + PERMISSIONS_ALL + "}"
                + "]}");
    }

    @Test
    public void testMappingFromAttribute() {
        ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

        app.getProtocolMappers().createMapper(createClaimMapper("user profile attribute precedence", "lastName", "firstName", "c_fn", "String", true, true, false)).close();

        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        Object firstName = idToken.getOtherClaims().get("c_fn");
        assertThat(firstName, instanceOf(String.class));
        assertThat(firstName, is("Tom"));

        oauth.openLogout();
    }

    @Test
    public void testFallbackToDefaultConfigProperty() {
        ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

        app.getProtocolMappers().createMapper(createClaimMapper("user profile default config property", "lastName", null, "c_fn_from_default", "String", true, true, false)).close();

        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        Object firstName = idToken.getOtherClaims().get("c_fn_from_default");
        assertThat(firstName, instanceOf(String.class));
        assertThat(firstName, is("Brady"));

        oauth.openLogout();
    }
}
