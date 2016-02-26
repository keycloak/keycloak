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

package org.keycloak.testsuite.admin;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Ignore
public class ClusteredConcurrencyTest {

    private static final Logger log = Logger.getLogger(ClusteredConcurrencyTest.class);

    private static final int DEFAULT_THREADS = 10;
    private static final int DEFAULT_ITERATIONS = 100;

    // If enabled only one request is allowed at the time. Useful for checking that test is working.
    private static final boolean SYNCHRONIZED = false;

    boolean passedCreateClient = false;
    boolean passedCreateRole = false;

    public static DefaultKeycloakSessionFactory node1factory;
    public static DefaultKeycloakSessionFactory node2factory;
    public static DefaultKeycloakSessionFactory[] nodes = new DefaultKeycloakSessionFactory[2];

    @BeforeClass
    public static void initKeycloak() throws Exception {
        System.setProperty("keycloak.connectionsInfinispan.clustered", "true");
        System.setProperty("keycloak.connectionsInfinispan.async", "false");
        KeycloakApplication.loadConfig();
        node1factory = new DefaultKeycloakSessionFactory();
        node1factory.init();
        nodes[0] = node1factory;
        node2factory = new DefaultKeycloakSessionFactory();
        node2factory.init();
        nodes[1] = node2factory;

        KeycloakSession session = nodes[0].create();
        session.getTransaction().begin();
        session.realms().createRealm("testrealm");
        session.getTransaction().commit();

        session = nodes[1].create();
        session.getTransaction().begin();
        RealmModel realm = session.realms().getRealmByName("testrealm");
        Assert.assertNotNull(realm);
        session.getTransaction().commit();

    }

    @Test
    public void createClient() throws Throwable {
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        run(new KeycloakRunnable() {
            @Override
            public void run(int threadNum, int iterationNum) {
                String name = "c-" + threadNum + "-" + iterationNum;
                int node1 = threadNum % 2;
                int node2 = 0;
                if (node1 == 0) node2 = 1;

                String id = null;
                {
                    KeycloakSession session = nodes[node1].create();
                    session.getTransaction().begin();
                    RealmModel realm = session.realms().getRealmByName("testrealm");
                    ClientModel client = realm.addClient(name);
                    id = client.getId();
                    session.getTransaction().commit();
                }
                {
                    KeycloakSession session = nodes[node2].create();
                    session.getTransaction().begin();
                    RealmModel realm = session.realms().getRealmByName("testrealm");
                    boolean found = false;
                    for (ClientModel client : realm.getClients()) {
                        if (client.getId().equals(id)) {
                            found = true;
                        }
                    }
                    session.getTransaction().commit();
                    if (!found) {
                        fail("Client " + name + " not found in client list");
                    }
                }
                {
                    KeycloakSession session = nodes[node1].create();
                    session.getTransaction().begin();
                    RealmModel realm = session.realms().getRealmByName("testrealm");
                    boolean found = false;
                    for (ClientModel client : realm.getClients()) {
                        if (client.getId().equals(id)) {
                            found = true;
                        }
                    }
                    session.getTransaction().commit();
                    if (!found) {
                        fail("Client " + name + " not found in client list");
                    }
                }
            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createClient took " + end);

    }

    private void run(final KeycloakRunnable runnable) throws Throwable {
        run(runnable, DEFAULT_THREADS, DEFAULT_ITERATIONS);
    }

    private void run(final KeycloakRunnable runnable, final int numThreads, final int numIterationsPerThread) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicReference<Throwable> failed = new AtomicReference();
        final List<Thread> threads = new LinkedList<>();
        final Lock lock = SYNCHRONIZED ? new ReentrantLock() : null;

        for (int t = 0; t < numThreads; t++) {
            final int threadNum = t;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        if (lock != null) {
                            lock.lock();
                        }

                        for (int i = 0; i < numIterationsPerThread && latch.getCount() > 0; i++) {
                            log.infov("thread {0}, iteration {1}", threadNum, i);
                            runnable.run(threadNum, i);
                        }
                        latch.countDown();
                    } catch (Throwable t) {
                        failed.compareAndSet(null, t);
                        while (latch.getCount() > 0) {
                            latch.countDown();
                        }
                    } finally {
                        if (lock != null) {
                            lock.unlock();
                        }
                    }
                }
            };
            thread.start();
            threads.add(thread);
        }

        latch.await();

        for (Thread t : threads) {
            t.join();
        }

        if (failed.get() != null) {
            throw failed.get();
        }
    }

    interface KeycloakRunnable {

        void run(int threadNum, int iterationNum);

    }

}
