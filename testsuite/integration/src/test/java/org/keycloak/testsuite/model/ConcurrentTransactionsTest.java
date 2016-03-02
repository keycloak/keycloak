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

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrentTransactionsTest extends AbstractModelTest {

    @Test
    public void persistClient() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        KeycloakSession session = realmManager.getSession();

        ClientModel client = session.realms().addClient(realm, "client");
        client.setSecret("old");

        String clientDBId = client.getId();

        final KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        commit();

        final CountDownLatch transactionsCounter = new CountDownLatch(2);
        final CountDownLatch readLatch = new CountDownLatch(1);
        final CountDownLatch updateLatch = new CountDownLatch(1);

        Thread thread1 = new Thread() {

            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        try {
                            // Wait until transaction in both threads started
                            transactionsCounter.countDown();
                            System.out.println("transaction1 started");
                            transactionsCounter.await();

                            // Read client
                            RealmModel realm = session.realms().getRealmByName("original");
                            ClientModel client = session.realms().getClientByClientId("client", realm);
                            System.out.println("transaction1: Read client finished");
                            readLatch.countDown();

                            // Wait until thread2 updates client and commits
                            updateLatch.await();
                            System.out.println("transaction1: Going to read client again");

                            client = session.realms().getClientByClientId("client", realm);
                            System.out.println("transaction1: secret: " + client.getSecret());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
            }

        };

        Thread thread2 = new Thread() {

            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        try {
                            // Wait until transaction in both threads started
                            transactionsCounter.countDown();
                            System.out.println("transaction2 started");
                            transactionsCounter.await();


                            readLatch.await();
                            System.out.println("transaction2: Going to update client secret");

                            RealmModel realm = session.realms().getRealmByName("original");
                            ClientModel client = session.realms().getClientByClientId("client", realm);
                            client.setSecret("new");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });

                System.out.println("transaction2: commited");
                updateLatch.countDown();
            }

        };

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("after thread join");

        commit();

        session = realmManager.getSession();

        realm = session.realms().getRealmByName("original");
        ClientModel clientFromCache = session.realms().getClientById(clientDBId, realm);
        ClientModel clientFromDB = session.getProvider(RealmProvider.class).getClientById(clientDBId, realm);

        System.out.println("SECRET FROM DB : " + clientFromDB.getSecret());
        System.out.println("SECRET FROM CACHE : " + clientFromCache.getSecret());

        Assert.assertEquals("new", clientFromDB.getSecret());
        Assert.assertEquals("new", clientFromCache.getSecret());
    }

}
