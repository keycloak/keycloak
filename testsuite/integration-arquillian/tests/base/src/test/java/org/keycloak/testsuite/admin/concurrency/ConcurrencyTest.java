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

package org.keycloak.testsuite.admin.concurrency;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ConcurrencyTest extends AbstractConcurrencyTest {

    boolean passedCreateClient = false;
    boolean passedCreateRole = false;

    //@Test
    public void testAllConcurrently() throws Throwable {
        Thread client = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createClient();
                    passedCreateClient = true;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });
        Thread role = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createRole();
                    passedCreateRole = true;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });

        client.start();
        role.start();
        client.join();
        role.join();
        Assert.assertTrue(passedCreateClient);
        Assert.assertTrue(passedCreateRole);
    }

    @Test
    public void createClient() throws Throwable {
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        run(new KeycloakRunnable() {
            @Override
            public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                String name = "c-" + threadNum + "-" + iterationNum;
                ClientRepresentation c = new ClientRepresentation();
                c.setClientId(name);
                Response response = realm.clients().create(c);
                String id = ApiUtil.getCreatedId(response);
                response.close();

                c = realm.clients().get(id).toRepresentation();
                assertNotNull(c);
                boolean found = false;
                for (ClientRepresentation r : realm.clients().findAll()) {
                    if (r.getClientId().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fail("Client " + name + " not found in client list");
                }
            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createClient took " + end);

    }

    @Test
    public void createGroup() throws Throwable {
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        run(new KeycloakRunnable() {
            @Override
            public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                String name = "c-" + threadNum + "-" + iterationNum;
                GroupRepresentation c = new GroupRepresentation();
                c.setName(name);
                Response response = realm.groups().add(c);
                String id = ApiUtil.getCreatedId(response);
                response.close();

                c = realm.groups().group(id).toRepresentation();
                assertNotNull(c);
                boolean found = false;
                for (GroupRepresentation r : realm.groups().groups()) {
                    if (r.getName().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fail("Group " + name + " not found in group list");
                }
            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createGroup took " + end);

    }

    @Test
    @Ignore
    public void createRemoveClient() throws Throwable {
        // FYI< this will fail as HSQL seems to be trying to perform table locks.
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        run(new KeycloakRunnable() {
            @Override
            public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                String name = "c-" + threadNum + "-" + iterationNum;
                ClientRepresentation c = new ClientRepresentation();
                c.setClientId(name);
                Response response = realm.clients().create(c);
                String id = ApiUtil.getCreatedId(response);
                response.close();

                c = realm.clients().get(id).toRepresentation();
                assertNotNull(c);
                boolean found = false;
                for (ClientRepresentation r : realm.clients().findAll()) {
                    if (r.getClientId().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fail("Client " + name + " not found in client list");
                }
                realm.clients().get(id).remove();
                try {
                    c = realm.clients().get(id).toRepresentation();
                    fail("Client " + name + " should not be found.  Should throw a 404");
                } catch (NotFoundException e) {

                }
                found = false;
                for (ClientRepresentation r : realm.clients().findAll()) {
                    if (r.getClientId().equals(name)) {
                        found = true;
                        break;
                    }
                }
                Assert.assertFalse("Client " + name + " should not be in client list", found);

            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createClient took " + end);

    }


    @Test
    public void createRole() throws Throwable {
        long start = System.currentTimeMillis();
        run(new KeycloakRunnable() {
            @Override
            public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                String name = "r-" + threadNum + "-" + iterationNum;
                RoleRepresentation r = new RoleRepresentation(name, null, false);
                realm.roles().create(r);
                assertNotNull(realm.roles().get(name).toRepresentation());
            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createRole took " + end);

    }

    @Test
    public void createClientRole() throws Throwable {
        long start = System.currentTimeMillis();
        ClientRepresentation c = new ClientRepresentation();
        c.setClientId("client");
        Response response = realm.clients().create(c);
        final String clientId = ApiUtil.getCreatedId(response);
        response.close();

        System.out.println("*********************************************");

        run(new KeycloakRunnable() {
            @Override
            public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                String name = "r-" + threadNum + "-" + iterationNum;
                RoleRepresentation r = new RoleRepresentation(name, null, false);

                ClientResource client = realm.clients().get(clientId);
                client.roles().create(r);

                assertNotNull(client.roles().get(name).toRepresentation());
            }
        });
        long end = System.currentTimeMillis() - start;
        System.out.println("createClientRole took " + end);
        System.out.println("*********************************************");

    }
}
