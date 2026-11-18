/*
 * Copyright 2026 Keycloak contributors.
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

package org.keycloak.tests.oauth;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = JWTAuthorizationGrantTransientUserTest.ServerConfig.class)
public class JWTAuthorizationGrantTransientUserTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = TransientRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void issuesStableSubjectWithoutPersistingUser() {
        int usersBefore = realm.admin().users().count();
        AccessToken first = issue("external-user");
        AccessToken second = issue("external-user");
        assertTrue(first.getSubject().startsWith("lightweight-"));
        assertEquals(first.getSubject(), second.getSubject());
        assertFalse(first.getPreferredUsername().contains("external-user"));
        assertEquals(usersBefore, realm.admin().users().count());
        assertTrue(realm.admin().users().search(first.getPreferredUsername()).isEmpty());
        String clientId = realm.admin().clients().findByClientId("test-app").get(0).getId();
        assertTrue(realm.admin().clients().get(clientId).getUserSessions(0, 100).isEmpty());
    }

    @Test
    public void transientModeDoesNotReuseLinkedUser() {
        AccessToken token = issue("basic-user-id");
        assertNotEquals(user.getId(), token.getSubject());
        assertNotEquals(user.getUsername(), token.getPreferredUsername());
    }

    @Test
    public void preservesSubjectCase() {
        assertNotEquals(issue("External-User").getSubject(), issue("external-user").getSubject());
    }

    @Test
    public void subjectDoesNotDependOnPreferredUsername() {
        AccessToken first = issue("external-user");
        JsonWebToken assertion = assertion("external-user");
        assertion.setOtherClaims("preferred_username", "a-different-name");
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion)).send();
        assertTrue(response.isSuccess(), response.getErrorDescription());
        AccessToken second = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        assertEquals(first.getSubject(), second.getSubject());
        assertEquals(first.getPreferredUsername(), second.getPreferredUsername());
    }

    @Test
    public void separatesIdenticalSubjectsFromDifferentIssuers() {
        AccessToken first = issue("external-user");
        String otherIssuer = "https://another-issuer.example";
        realm.updateIdentityProvider(IDP_ALIAS, rep -> rep.getConfig().put(IdentityProviderModel.ISSUER, otherIssuer));
        JsonWebToken assertion = createAuthorizationGrantToken("external-user", oAuthClient.getEndpoints().getIssuer(), otherIssuer);
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion)).send();
        assertTrue(response.isSuccess(), response.getErrorDescription());
        AccessToken second = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        assertNotEquals(first.getSubject(), second.getSubject());
    }

    @Test
    public void rejectsExpiredAssertion() {
        JsonWebToken assertion = assertion("external-user");
        assertion.exp((long) Time.currentTime() - 1);
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion)).send();
        assertFailure("Token is not active", response, events.poll());
    }

    @Test
    public void rejectsUnknownUserWhenOptInIsDisabled() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> rep.getConfig().put(IdentityProviderModel.DO_NOT_STORE_USERS, "false"));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion("external-user"))).send();
        assertFailure("User not found", response, events.poll());
    }

    @Test
    public void rejectsInvalidAudience() {
        JsonWebToken assertion = assertion("external-user");
        assertion.audience("https://wrong-audience.example");
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion)).send();
        assertFailure("Invalid token audience", response, events.poll());
    }

    @Test
    public void rejectsInvalidSignatureWithoutPersistingUser() {
        int usersBefore = realm.admin().users().count();
        String jwt = getIdentityProvider().encodeToken(assertion("external-user"));
        String invalidSignature = jwt.substring(0, jwt.lastIndexOf('.') + 1) + "aW52YWxpZA";
        assertFalse(oAuthClient.jwtAuthorizationGrantRequest(invalidSignature).send().isSuccess());
        assertEquals(usersBefore, realm.admin().users().count());
    }

    @Test
    public void rejectsAssertionReuse() {
        String assertion = getIdentityProvider().encodeToken(assertion("external-user"));
        assertTrue(oAuthClient.jwtAuthorizationGrantRequest(assertion).send().isSuccess());
        events.poll();
        assertFalse(oAuthClient.jwtAuthorizationGrantRequest(assertion).send().isSuccess());
    }

    @Test
    public void doesNotImportAssertionClaimsAsRolesOrAttributes() {
        JsonWebToken assertion = assertion("external-user");
        assertion.setOtherClaims("email", "unmapped@example.org");
        assertion.setOtherClaims("realm_access", java.util.Map.of("roles", java.util.List.of("admin")));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion)).send();
        assertTrue(response.isSuccess(), response.getErrorDescription());
        AccessToken token = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        assertNull(token.getEmail());
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("admin"));
    }

    private JsonWebToken assertion(String subject) {
        return createAuthorizationGrantToken(subject, oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER);
    }

    private AccessToken issue(String subject) {
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(getIdentityProvider().encodeToken(assertion(subject))).send();
        assertTrue(response.isSuccess(), response.getErrorDescription());
        assertNull(response.getRefreshToken());
        AccessToken token = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        assertNull(token.getSessionId());
        events.poll();
        return token;
    }

    public static class TransientRealmConfig extends JWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);
            realm.build().getIdentityProviders().stream()
                    .filter(idp -> IDP_ALIAS.equals(idp.getAlias()))
                    .forEach(idp -> idp.getConfig().put(IdentityProviderModel.DO_NOT_STORE_USERS, "true"));
            return realm;
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.TRANSIENT_USERS);
        }
    }
}
