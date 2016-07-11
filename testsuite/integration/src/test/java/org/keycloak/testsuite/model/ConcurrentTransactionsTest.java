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
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrentTransactionsTest extends AbstractModelTest {

    private static final Logger logger = Logger.getLogger(ConcurrentTransactionsTest.class);

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
                            logger.info("transaction1 started");
                            transactionsCounter.await();

                            // Read client
                            RealmModel realm = session.realms().getRealmByName("original");
                            ClientModel client = session.realms().getClientByClientId("client", realm);
                            logger.info("transaction1: Read client finished");
                            readLatch.countDown();

                            // Wait until thread2 updates client and commits
                            updateLatch.await();
                            logger.info("transaction1: Going to read client again");

                            client = session.realms().getClientByClientId("client", realm);
                            logger.info("transaction1: secret: " + client.getSecret());
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
                            logger.info("transaction2 started");
                            transactionsCounter.await();


                            readLatch.await();
                            logger.info("transaction2: Going to update client secret");

                            RealmModel realm = session.realms().getRealmByName("original");
                            ClientModel client = session.realms().getClientByClientId("client", realm);
                            client.setSecret("new");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });

                logger.info("transaction2: commited");
                updateLatch.countDown();
            }

        };

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        logger.info("after thread join");

        commit();

        session = realmManager.getSession();

        realm = session.realms().getRealmByName("original");
        ClientModel clientFromCache = session.realms().getClientById(clientDBId, realm);
        ClientModel clientFromDB = session.getProvider(RealmProvider.class).getClientById(clientDBId, realm);

        logger.info("SECRET FROM DB : " + clientFromDB.getSecret());
        logger.info("SECRET FROM CACHE : " + clientFromCache.getSecret());

        Assert.assertEquals("new", clientFromDB.getSecret());
        Assert.assertEquals("new", clientFromCache.getSecret());
    }


    // KEYCLOAK-3296
    @Test
    public void removeUserAttribute() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        KeycloakSession session = realmManager.getSession();

        UserModel user = session.users().addUser(realm, "john");
        user.setSingleAttribute("foo", "val1");

        final KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        commit();

        AtomicReference<Exception> reference = new AtomicReference<>();

        final CountDownLatch readAttrLatch = new CountDownLatch(2);

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                        @Override
                        public void run(KeycloakSession session) {
                            try {
                                // Read user attribute
                                RealmModel realm = session.realms().getRealmByName("original");
                                UserModel john = session.users().getUserByUsername("john", realm);
                                String attrVal = john.getFirstAttribute("foo");

                                // Wait until it's read in both threads
                                readAttrLatch.countDown();
                                readAttrLatch.await();

                                // Remove user attribute in both threads
                                john.removeAttribute("foo");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                    });
                } catch (Exception e) {
                    reference.set(e);
                    throw new RuntimeException(e);
                } finally {
                    readAttrLatch.countDown();
                }
            }

        };

        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        logger.info("removeUserAttribute: after thread join");

        commit();

        if (reference.get() != null) {
            Assert.fail("Exception happened in some of threads. Details: " + reference.get().getMessage());
        }
    }

}
