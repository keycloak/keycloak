/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.sync;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SyncDummyUserFederationProviderFactory extends DummyUserFederationProviderFactory {

    // Used during SyncFederationTest
    public static volatile CountDownLatch latch1 = new CountDownLatch(1);
    public static volatile CountDownLatch latch2 = new CountDownLatch(1);

    public static void restartLatches() {
        latch1 = new CountDownLatch(1);
        latch2 = new CountDownLatch(1);
    }



    private static final Logger logger = Logger.getLogger(SyncDummyUserFederationProviderFactory.class);

    public static final String SYNC_PROVIDER_ID = "sync-dummy";
    public static final String WAIT_TIME = "wait-time"; // waitTime before transaction is commited

    @Override
    public String getId() {
        return SYNC_PROVIDER_ID;
    }


    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name("important.config")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(WAIT_TIME)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();
    }


    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                int waitTime = Integer.parseInt(model.getConfig().getFirst(WAIT_TIME));

                logger.infof("Starting sync of changed users. Wait time is: %s", waitTime);

                RealmModel realm = session.realms().getRealm(realmId);

                // KEYCLOAK-2412 : Just remove and add some users for testing purposes
                for (int i = 0; i < 10; i++) {
                    String username = "dummyuser-" + i;
                    UserModel user = session.userLocalStorage().getUserByUsername(realm, username);

                    if (user != null) {
                        session.userLocalStorage().removeUser(realm, user);
                    }

                    user = session.userLocalStorage().addUser(realm, username);
                }

                logger.infof("Finished sync of changed users. Waiting now for %d seconds", waitTime);


                try {
                    latch1.await(waitTime * 1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted!", ie);
                }

                logger.infof("Finished waiting");
            }

        });

        // countDown, so the SyncFederationTest can continue
        latch2.countDown();

        return new SynchronizationResult();
    }

}
