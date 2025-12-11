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

package org.keycloak.testsuite.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributeSelector;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest extends AbstractTestRealmKeycloakTest {

    @Test
    public void demoDelete() {
        // was having trouble deleting this realm from admin console
        removeRealm("demo-delete");
    }

	@Test
    public void install2() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("demo");

            Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
            Assert.assertEquals(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT, realm.getAccessTokenLifespanForImplicitFlow());
            Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT, realm.getOfflineSessionIdleTimeout());
            Assert.assertEquals(1, realm.getRequiredCredentialsStream().count());
            Assert.assertEquals("password", realm.getRequiredCredentialsStream().findFirst().get().getType());
        });
    }

    // KEYCLOAK-12921 NPE importing realm with no request context
    @Test
    public void importWithoutRequestContext() throws IOException {
        final String realmString = IOUtils.toString(getClass().getResourceAsStream("/model/realm-validation.json"), StandardCharsets.UTF_8);

        testingClient.server().run(session -> {
            RealmRepresentation testRealm = JsonSerialization.readValue(realmString, RealmRepresentation.class);

            AtomicReference<Throwable> err = new AtomicReference<>();

            // Need a new thread to not get context from thread processing request to run-on-server endpoint
            Thread t = new Thread(() -> {
                RealmModel realmModel;
                try (KeycloakSession ses = session.getKeycloakSessionFactory().create()) {
                    ses.getContext().setRealm(session.getContext().getRealm());
                    ses.getTransactionManager().begin();

                    realmModel = new RealmManager(ses).importRealm(testRealm);
                }

                try (KeycloakSession ses = session.getKeycloakSessionFactory().create()) {
                    ses.getTransactionManager().begin();
                    session.realms().removeRealm(realmModel.getId());
                } catch (Throwable th) {
                    err.set(th);
                }
            });

            synchronized (t) {
                t.start();
                try {
                    t.wait(10000);
                } catch (InterruptedException e) {
                    throw new RunOnServerException(e);
                }
            }

            if (err.get() != null) {
                throw new RunOnServerException(err.get());
            }
        });
    }

    // KEYCLOAK-12640
    @Test
    public void importAuthorizationSettings() throws Exception {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.AUTHORIZATION);

        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/model/authz-bug.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("authz-bug");
            AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
            ClientModel client = realm.getClientByClientId("appserver");
            ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(client);
            Assert.assertEquals("AFFIRMATIVE", resourceServer.getDecisionStrategy().name());
        });
    }

    // https://github.com/keycloak/keycloak/issues/32799
    @Test
    public void importAcrToLoaMappingWithDefaultAcrValues() {
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/model/acr-values-import-bug.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("acr-import-bug");
            Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(realm);
            Assert.assertNotNull(acrLoaMap);

            ClientModel clientSilverAcr = realm.getClientByClientId("client-silver");
            Assert.assertEquals("silver", clientSilverAcr.getAttribute("default.acr.values"));
        });
    }

    // https://github.com/keycloak/keycloak/issues/10730
    @Test
    public void importLdapWithReferenceToGroupBeingImported() {
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/model/testrealm-ldap-group.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("ldap-group-import-bug");

            Optional<ComponentModel> hardCodedGroup = realm.getComponentsStream()
                    .filter((component) -> component.getName().equals("hard-coded-group"))
                    .findFirst();


            Assert.assertTrue(hardCodedGroup.isPresent());
        });
    }

    @Test
    public void importUserProfile() throws Exception {
        final String realmString = IOUtils.toString(getClass().getResourceAsStream("/model/import-userprofile.json"), StandardCharsets.UTF_8);

        testingClient.server().run(session -> {
            RealmRepresentation realmRep = JsonSerialization.readValue(realmString, RealmRepresentation.class);

            // make sure the import happens within the context of the realm being imported
            session.getContext().setRealm(null);
            ImportUtils.importRealm(session, realmRep, Strategy.OVERWRITE_EXISTING, true);

            RealmModel realm = session.realms().getRealmByName(realmRep.getRealm());

            session.getContext().setRealm(realm);

            UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
            UPConfig config = provider.getConfiguration();

            Assert.assertTrue(config.getAttributes().stream().map(UPAttribute::getName).anyMatch("email"::equals));
            Assert.assertTrue(config.getAttributes().stream().map(UPAttribute::getName).anyMatch("test"::equals));
            Assert.assertTrue(config.getAttributes().stream().map(UPAttribute::getSelector)
                    .filter(Objects::nonNull)
                    .map(UPAttributeSelector::getScopes)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList())
                    .contains("microprofile-jwt")
            );
        });
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealmParm) {

        log.infof("testrealm2 imported");
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/model/testrealm2.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);

        log.infof("testrealm-demo imported");
        testRealm = loadJson(getClass().getResourceAsStream("/model/testrealm-demo.json"), RealmRepresentation.class);
        testRealm.setRealm("demo");
        testRealm.setId("demo");
        adminClient.realms().create(testRealm);
    }

}
