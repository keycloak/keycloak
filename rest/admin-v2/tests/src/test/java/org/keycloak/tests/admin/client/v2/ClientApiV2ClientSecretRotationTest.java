/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client.v2;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test client secret rotation and client admin api v2.
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = ClientApiV2ClientSecretRotationTest.TestServerConfig.class)
public class ClientApiV2ClientSecretRotationTest extends AbstractClientApiV2Test {


    @InjectRealm(config = TestRealmConfig.class, ref = "testRealm")
    ManagedRealm realm;

    @InjectRunOnServer(realmRef = "testRealm")
    RunOnServerClient runOnServer;

    @Override
    public String getRealmName() {
        return realm.getName();
    }

    @Test
    public void testRotation() throws Exception {
        var clientId = "other";
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId(clientId);
        rep.setDescription("I'm new");
        rep.setAuth(new OIDCClientRepresentation.Auth());
        rep.getAuth().setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        rep.getAuth().setSecret(null);

        // create the client with auto-generated secret
        OIDCClientRepresentation createdClient;
        try (var response = getClientsApi().createClient(rep)) {
            createdClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(createdClient.getDescription(), is("I'm new"));
            assertThat(createdClient.getAuth(), notNullValue());
            assertThat(createdClient.getAuth().getSecret(), Matchers.not(emptyOrNullString()));
            checkRotatedInfoOnlySecretPresent(createdClient.getUuid(), createdClient.getAuth().getSecret());
            rep = createdClient;
        }

        // update just description should not change the rotation info and keep the same secret
        OIDCClientRepresentation updatedClient;
        rep.setDescription("updated2");
        try (var response = getClientsApi().client(clientId).createOrUpdateClient(rep)) {
            assertThat(response.getStatus(), is(200));
            updatedClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updatedClient.getDescription(), is("updated2"));
            assertThat(updatedClient.getAuth().getSecret(), is(createdClient.getAuth().getSecret()));
            checkRotatedInfoOnlySecretPresent(updatedClient.getUuid(), createdClient.getAuth().getSecret());
        }

        // force new password not using patch changes the password without rotation
        rep.setDescription("updated3");
        rep.getAuth().setSecret("new-super-secure-secret");
        try (var response = getClientsApi().client(clientId).createOrUpdateClient(rep)) {
            assertThat(response.getStatus(), is(200));
            updatedClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updatedClient.getDescription(), is("updated3"));
            assertThat(updatedClient.getAuth().getSecret(), is("new-super-secure-secret"));
            checkRotatedInfoOnlySecretPresent(updatedClient.getUuid(), "new-super-secure-secret");
        }

        // update the password forcing the rotation using patch
        String body = String.format(Locale.ROOT,
                "{\"enabled\":true,\"clientId\":\"%s\",\"description\":\"updated4\",\"auth\":{\"method\":\"%s\",\"secret\":null}}",
                clientId, ClientIdAndSecretAuthenticator.PROVIDER_ID);
        updatedClient = (OIDCClientRepresentation) getClientsApi().client(clientId)
                .patchClient(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        assertThat(updatedClient.getDescription(), is("updated4"));
        assertThat(updatedClient.getAuth(), notNullValue());
        String newlyGeneratedSecret = updatedClient.getAuth().getSecret();
        assertThat(newlyGeneratedSecret, not(emptyOrNullString()));
        assertThat(newlyGeneratedSecret, not(is("new-super-secure-secret")));
        checkRotatedInfoBothPresent(updatedClient.getUuid(), newlyGeneratedSecret, "new-super-secure-secret");
        rep = updatedClient;

        rep.setDescription("updated5");
        try (var response = getClientsApi().client(clientId).createOrUpdateClient(rep)) {
            updatedClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(response.getStatus(), is(200));
            assertThat(updatedClient.getDescription(), is("updated5"));
            assertThat(updatedClient.getAuth().getSecret(), is(newlyGeneratedSecret));
            checkRotatedInfoBothPresent(updatedClient.getUuid(), newlyGeneratedSecret, "new-super-secure-secret");
        }

        // disable the policy and check rotation data is removed and same password remains
        disablePolicy();
        rep.setDescription("updated6");
        try (var response = getClientsApi().client(clientId).createOrUpdateClient(rep)) {
            assertThat(response.getStatus(), is(200));
            updatedClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updatedClient.getDescription(), is("updated6"));
            assertThat(updatedClient.getAuth().getSecret(), is(newlyGeneratedSecret));
            checkRotatedInfoRemoved(updatedClient.getUuid(), newlyGeneratedSecret);
        }
    }

    private void checkRotatedInfoOnlySecretPresent(String uuid, String secret) {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            ClientModel clientModel = session.clients().getClientById(realmModel, uuid);
            OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(clientModel);
            assertThat(wrapper.getSecret(), is(secret));
            assertThat(wrapper.getClientRotatedSecret(session, false), nullValue());
            assertThat(wrapper.getClientRotatedSecretCreationTime(), is(0L));
            assertThat(wrapper.getClientRotatedSecretExpirationTime(), is(0L));
            assertThat(wrapper.getClientSecretCreationTime(), greaterThan(0L));
            assertThat(wrapper.getClientSecretExpirationTime(), greaterThan(0L));
        });
    }

    private void checkRotatedInfoBothPresent(String uuid, String secret, String rotatedSecret) {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            ClientModel clientModel = session.clients().getClientById(realmModel, uuid);
            OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(clientModel);
            assertThat(wrapper.getSecret(), is(secret));
            assertThat(wrapper.getClientRotatedSecret(session), is(rotatedSecret));
            assertThat(wrapper.getClientRotatedSecretCreationTime(), greaterThan(0L));
            assertThat(wrapper.getClientRotatedSecretExpirationTime(), greaterThan(0L));
            assertThat(wrapper.getClientSecretCreationTime(), greaterThan(0L));
            assertThat(wrapper.getClientSecretExpirationTime(), greaterThan(0L));
        });
    }

    private void checkRotatedInfoRemoved(String uuid, String secret) {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            ClientModel clientModel = session.clients().getClientById(realmModel, uuid);
            OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(clientModel);
            assertThat(wrapper.getSecret(), is(secret));
            assertThat(wrapper.getClientRotatedSecret(session), nullValue());
            assertThat(wrapper.getClientRotatedSecretCreationTime(), is(0L));
            assertThat(wrapper.getClientRotatedSecretExpirationTime(), is(0L));
            assertThat(wrapper.getClientSecretCreationTime(), greaterThan(0L));
            assertThat(wrapper.getClientSecretExpirationTime(), is(0L));
        });
    }

    private void disablePolicy() {
        realm.updateWithCleanup(r -> {
            return r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                            .name("ClientSecretRotationPolicy")
                            .description("Policy for Client Secret Rotation")
                            .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                                    false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                            .profile("ClientSecretRotationProfile")
                            .enabled(false)
                            .build());
        });
    }

    public static class TestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }

    public static class TestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            var profileConfig = new ClientSecretRotationExecutor.Configuration();
            profileConfig.setExpirationPeriod(TimeUnit.HOURS.toSeconds(1));
            profileConfig.setRotatedExpirationPeriod(TimeUnit.MINUTES.toSeconds(10));
            profileConfig.setRemainExpirationPeriod(TimeUnit.MINUTES.toSeconds(30));

            realm.clients(ClientBuilder.create("myclient")
                    .secret("mysecret")
                    .serviceAccountsEnabled(true));
            realm.clientProfile(ClientProfileBuilder.create()
                    .name("ClientSecretRotationProfile")
                    .description("Enable Client Secret Rotation")
                    .executor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                    .build());
            realm.clientPolicy(ClientPolicyBuilder.create()
                    .name("ClientSecretRotationPolicy")
                    .description("Policy for Client Secret Rotation")
                    .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                            false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                    .profile("ClientSecretRotationProfile")
                    .build());
            return realm;
        }
    }
}
