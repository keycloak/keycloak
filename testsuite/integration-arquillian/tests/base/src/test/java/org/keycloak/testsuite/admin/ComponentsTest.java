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

import org.keycloak.admin.client.resource.ComponentResource;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.components.TestProvider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ComponentsTest extends AbstractAdminTest {

    private ComponentsResource components;

    @Before
    public void before() throws Exception {
        components = adminClient.realm(REALM_NAME).components();
    }

    private volatile CountDownLatch remainingDeleteSubmissions;

    private static final int NUMBER_OF_THREADS = 4;
    private static final int NUMBER_OF_TASKS = NUMBER_OF_THREADS * 5;
    private static final int NUMBER_OF_CHILDREN = 3;

    private void testConcurrency(BiConsumer<ExecutorService, Integer> taskCreator) throws InterruptedException {
        ExecutorService s = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
          new BasicThreadFactory.Builder().daemon(true).uncaughtExceptionHandler((t, e) -> log.error(e.getMessage(), e)).build());
        this.remainingDeleteSubmissions = new CountDownLatch(NUMBER_OF_TASKS);

        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            taskCreator.accept(s, i);
        }

        try {
            assertTrue("Did not create all components in time", this.remainingDeleteSubmissions.await(100, TimeUnit.SECONDS));
            s.shutdown();
            assertTrue("Did not finish before timeout", s.awaitTermination(100, TimeUnit.SECONDS));
        } finally {
            s.shutdownNow();
        }
    }

    @Test
    public void testConcurrencyWithoutChildren() throws InterruptedException {
        testConcurrency((s, i) -> s.submit(new CreateAndDeleteComponent(s, i)));

        assertThat(realm.components().query(realm.toRepresentation().getId(), TestProvider.class.getName()), Matchers.hasSize(0));
    }

    @Test
    public void testConcurrencyWithChildren() throws InterruptedException {
        testConcurrency((s, i) -> s.submit(new CreateAndDeleteComponentWithFlatChildren(s, i)));

        assertThat(realm.components().query(realm.toRepresentation().getId(), TestProvider.class.getName()), Matchers.hasSize(0));
    }

    @Test
    public void testNotDeadlocked() {
        for (int i = 0; i < 50; i++) {
            ComponentRepresentation rep = createComponentRepresentation("test-" + i);
            rep.getConfig().putSingle("required", "required-value");
            createComponent(rep);

            List<ComponentRepresentation> list = realm.components().query(realmId, TestProvider.class.getName());
            assertEquals(i + 1, list.size());
        }
    }

    @Test
    public void testCreateValidation() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        // Check validation is invoked
        try {
            createComponent(rep);
        } catch (WebApplicationException e) {
            assertError(e.getResponse(), "'Required' is required");
        }

        rep.getConfig().putSingle("required", "Required");
        rep.getConfig().putSingle("number", "invalid");

        // Check validation is invoked
        try {
            createComponent(rep);
        } catch (WebApplicationException e) {
            assertError(e.getResponse(), "'Number' should be a number");
        }
    }

    @Test
    public void testCreateEmptyValues() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", "");
        rep.getConfig().put("val2", null);
        rep.getConfig().put("val3", Collections.emptyList());

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals( "foo", returned.getSubType());

        assertEquals(1, returned.getConfig().size());
        assertTrue(returned.getConfig().containsKey("required"));
    }

    @Test
    public void testCreateWithoutGivenId() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        rep.getConfig().addFirst("required", "foo");
        rep.setId(null);

        String id = createComponent(rep);
        assertNotNull(id);
    }

    @Test
    public void testCreateWithGivenId() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        rep.getConfig().addFirst("required", "foo");
        rep.setId("fixed-id");

        String id = createComponent(rep);
        assertEquals("fixed-id", id);
    }

    @Test
    public void testUpdate() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", "one");
        rep.getConfig().addFirst("val2", "two");
        rep.getConfig().addFirst("val3", "three");

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(4, returned.getConfig().size());
        assertEquals("foo", returned.getConfig().getFirst("required"));
        assertEquals("one", returned.getConfig().getFirst("val1"));
        assertEquals("two", returned.getConfig().getFirst("val2"));
        assertEquals("three", returned.getConfig().getFirst("val3"));

        // Check value updated
        returned.getConfig().putSingle("val1", "one-updated");

        // Check null deletes property
        returned.getConfig().putSingle("val2", null);

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(3, returned.getConfig().size());
        assertEquals("one-updated", returned.getConfig().getFirst("val1"));
        assertFalse(returned.getConfig().containsKey("val2"));

        // Check empty string is deleted
        returned.getConfig().addFirst("val1", "");

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(2, returned.getConfig().size());

        // Check empty list removes property
        returned.getConfig().put("val3", Collections.emptyList());

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(1, returned.getConfig().size());
    }


    @Test
    public void testRename() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        rep.getConfig().addFirst("required", "foo");

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals("mycomponent", returned.getName());

        rep.setName("myupdatedcomponent");

        components.component(id).update(rep);

        returned = components.component(id).toRepresentation();
        assertEquals("myupdatedcomponent", returned.getName());
    }

    @Test
    public void testSecretConfig() throws Exception {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        rep.getConfig().addFirst("secret", "some secret value!!");
        rep.getConfig().addFirst("required", "some required value");

        String id = createComponent(rep);

        // Check secret value is not returned
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned.getConfig().getFirst("secret"));

        // Check secret not leaked in admin events
        AdminEventRepresentation event = testingClient.testing().pollAdminEvent();
        assertFalse(event.getRepresentation().contains("some secret value!!"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        Map<String, TestProvider.DetailsRepresentation> details = testingClient.testing(REALM_NAME).getTestComponentDetails();

        // Check value is set correctly
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned.getConfig().putSingle("priority", "200");
        components.component(id).update(returned);

        ComponentRepresentation returned2 = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned2.getConfig().getFirst("secret"));

        // Check secret not leaked in admin events
        event = testingClient.testing().pollAdminEvent();
        assertThat(event.getRepresentation(), not(containsString("some secret value!!")));
        assertThat(event.getRepresentation(), containsString(ComponentRepresentation.SECRET_VALUE));

        // Check secret value is not set to '*********'
        details = testingClient.testing(REALM_NAME).getTestComponentDetails();
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned2.getConfig().putSingle("secret", "updated secret value!!");
        components.component(id).update(returned2);

        // Check secret value is updated
        details = testingClient.testing(REALM_NAME).getTestComponentDetails();
        assertEquals("updated secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        ComponentRepresentation returned3 = components.query().stream().filter(c -> c.getId().equals(returned2.getId())).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned3.getConfig().getFirst("secret"));


        returned2.getConfig().putSingle("secret", "${vault.value}");
        components.component(id).update(returned2);

        // Check secret value is updated
        details = testingClient.testing(REALM_NAME).getTestComponentDetails();
        assertThat(details.get("mycomponent").getConfig().get("secret"), contains("${vault.value}"));

        ComponentRepresentation returned4 = components.query().stream().filter(c -> c.getId().equals(returned2.getId())).findFirst().get();
        assertThat(returned4.getConfig().get("secret"), contains("${vault.value}"));
    }

    @Test
    public void testLongValueInComponentConfigAscii() throws Exception {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        String value = StringUtils.repeat("0123456789", 400);  // 4000 8-bit characters

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", value);

        String id = createComponent(rep);

        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(value, returned.getConfig().getFirst("val1"));
    }

    @Test
    public void testLongValueInComponentConfigExtLatin() throws Exception {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        String value = StringUtils.repeat("ěščřžýíŮÍÁ", 400);  // 4000 Unicode extended-Latin characters

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", value);

        String id = createComponent(rep);

        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(value, returned.getConfig().getFirst("val1"));
    }

    private java.lang.String createComponent(ComponentRepresentation rep) {
        return createComponent(realm, rep);
    }

    private String createComponent(RealmResource realm, ComponentRepresentation rep) {
        Response response = null;
        try {
            ComponentsResource components = realm.components();
            response = components.add(rep);
            String id = ApiUtil.getCreatedId(response);
            getCleanup(realm.toRepresentation().getRealm()).addComponentId(id);
            return id;
        } finally {
            if (response != null) {
                response.bufferEntity();
                response.close();
            }
        }
    }

    private void assertError(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
    }

    private ComponentRepresentation createComponentRepresentation(String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realmId);
        rep.setProviderId("test");
        rep.setProviderType(TestProvider.class.getName());
        rep.setSubType("foo");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        rep.setConfig(config);
        return rep;
    }

    private class CreateComponent implements Runnable {

        protected final ExecutorService s;
        protected final int i;
        protected final RealmResource realm;

        public CreateComponent(ExecutorService s, int i, RealmResource realm) {
            this.s = s;
            this.i = i;
            this.realm = realm;
        }

        public CreateComponent(ExecutorService s, int i) {
            this(s, i, ComponentsTest.this.realm);
        }

        @Override
        public void run() {
            log.debugf("Started for i=%d ", i);
            ComponentRepresentation rep = createComponentRepresentation("test-" + i);
            rep.getConfig().putSingle("required", "required-value");
            rep.setParentId(this.realm.toRepresentation().getId());
            
            String id = createComponent(this.realm, rep);
            assertThat(id, Matchers.notNullValue());

            createChildren(id);

            log.debugf("Finished: i=%d, id=%s", i, id);

            scheduleDeleteComponent(id);
            remainingDeleteSubmissions.countDown();
        }

        protected void scheduleDeleteComponent(String id) {
        }

        protected void createChildren(String id) {
        }
    }

    private class CreateAndDeleteComponent extends CreateComponent {

        public CreateAndDeleteComponent(ExecutorService s, int i) {
            super(s, i);
        }

        @Override
        protected void scheduleDeleteComponent(String id) {
            s.submit(new DeleteComponent(id));
        }
    }

    private class CreateComponentWithFlatChildren extends CreateComponent {

        public CreateComponentWithFlatChildren(ExecutorService s, int i, RealmResource realm) {
            super(s, i, realm);
        }

        public CreateComponentWithFlatChildren(ExecutorService s, int i) {
            super(s, i);
        }

        @Override
        protected void createChildren(String id) {
            for (int j = 0; j < NUMBER_OF_CHILDREN; j ++) {
                ComponentRepresentation rep = createComponentRepresentation("test-" + i + ":" + j);
                rep.setParentId(id);
                rep.getConfig().putSingle("required", "required-value");

                assertThat(createComponent(this.realm, rep), Matchers.notNullValue());
            }
        }

    }

    private class CreateAndDeleteComponentWithFlatChildren extends CreateAndDeleteComponent {

        public CreateAndDeleteComponentWithFlatChildren(ExecutorService s, int i) {
            super(s, i);
        }

        @Override
        protected void createChildren(String id) {
            for (int j = 0; j < NUMBER_OF_CHILDREN; j ++) {
                ComponentRepresentation rep = createComponentRepresentation("test-" + i + ":" + j);
                rep.setParentId(id);
                rep.getConfig().putSingle("required", "required-value");

                assertThat(createComponent(this.realm, rep), Matchers.notNullValue());
            }
        }

    }

    private class DeleteComponent implements Runnable {

        private final String id;

        public DeleteComponent(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            log.debugf("Started, id=%s", id);

            ComponentResource c = realm.components().component(id);
            assertThat(c.toRepresentation(), Matchers.notNullValue());
            c.remove();

            log.debugf("Finished, id=%s", id);
        }
    }

}
