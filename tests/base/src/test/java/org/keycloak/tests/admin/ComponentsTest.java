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

package org.keycloak.tests.admin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.components.TestComponentProvider;
import org.keycloak.testsuite.components.TestComponentProviderFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = ComponentsTest.ComponentsTestServerConfig.class)
public class ComponentsTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private static final Logger log = Logger.getLogger(ComponentsTest.class);

    private ComponentsResource components;

    @BeforeEach
    public void before() throws Exception {
        components = managedRealm.admin().components();
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
            assertTrue(this.remainingDeleteSubmissions.await(100, TimeUnit.SECONDS), "Did not create all components in time");
            s.shutdown();
            assertTrue(s.awaitTermination(100, TimeUnit.SECONDS), "Did not finish before timeout");
        } finally {
            s.shutdownNow();
        }
    }

    @Test
    public void testConcurrencyWithoutChildren() throws InterruptedException {
        testConcurrency((s, i) -> s.submit(new CreateAndDeleteComponent(s, i)));

//        Data consistency is not guaranteed with concurrent access to entities in map store.
//        For details see https://issues.redhat.com/browse/KEYCLOAK-17586
//        The reason that this test remains here is to test whether it finishes in time (we need to test whether there is no slowness).
//        assertThat(realm.components().query(realm.toRepresentation().getId(), TestProvider.class.getName()), Matchers.hasSize(0));
    }

    @Test
    public void testConcurrencyWithChildren() throws InterruptedException {
        testConcurrency((s, i) -> s.submit(new CreateAndDeleteComponentWithFlatChildren(s, i)));

//        Data consistency is not guaranteed with concurrent access to entities in map store.
//        For details see https://issues.redhat.com/browse/KEYCLOAK-17586
//        The reason that this test remains here is to test whether it finishes in time (we need to test whether there is no slowness).
//        assertThat(realm.components().query(realm.toRepresentation().getId(), TestProvider.class.getName()), Matchers.hasSize(0));
    }

    @Test
    public void testNotDeadlocked() {
        for (int i = 0; i < 50; i++) {
            ComponentRepresentation rep = createComponentRepresentation("test-" + i);
            rep.getConfig().putSingle("required", "required-value");
            createComponent(rep);

            List<ComponentRepresentation> list = components.query(managedRealm.getId(), TestComponentProvider.class.getName());
            assertEquals(i + 1, list.size());
        }
    }

    @Test
    public void testCreateValidation() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        // Check validation is invoked
        createComponentAndAssertError(rep, "'Required' is required");

        rep.getConfig().putSingle("required", "Required");
        rep.getConfig().putSingle("number", "invalid");

        // Check validation is invoked
        createComponentAndAssertError(rep, "'Number' should be a number");
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
        String componentId = KeycloakModelUtils.generateId();
        rep.setId(componentId);

        String id = createComponent(rep);
        assertEquals(componentId, id);
    }

    @Test
    public void failCreateWithLongName() {
        StringBuilder name = new StringBuilder();

        while (name.length() < 30) {
            name.append("invalid");
        }

        ComponentRepresentation rep = createComponentRepresentation(name.toString());

        rep.getConfig().addFirst("required", "foo");

        try (Response response = components.add(rep)) {
            if (Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() == response.getStatus()) {
                // using database should fail due to constraint violations
                assertFalse(components.query().stream().map(ComponentRepresentation::getName).anyMatch(name.toString()::equals));
            } else if (Response.Status.CREATED.getStatusCode() == response.getStatus()) {
                // using the map storage should work because there are no constraints
                String id = ApiUtil.getCreatedId(response);
                assertNotNull(components.component(id).toRepresentation());
            } else {
                fail("Unexpected response");
            }
        }
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

        // send a key / value which not contained in the original component config
        returned.getConfig().putSingle("not-a-config-key", "ten");

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
        AdminEventRepresentation event = adminEvents.poll();
        assertFalse(event.getRepresentation().contains("some secret value!!"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        Map<String, TestComponentProvider.DetailsRepresentation> details = runOnServer.fetch(new TestComponents()).componentsDetailsMap();

        // Check value is set correctly
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned.getConfig().putSingle("priority", "200");
        components.component(id).update(returned);

        ComponentRepresentation returned2 = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned2.getConfig().getFirst("secret"));

        // Check secret not leaked in admin events
        event = adminEvents.poll();
        assertThat(event.getRepresentation(), not(containsString("some secret value!!")));
        assertThat(event.getRepresentation(), containsString(ComponentRepresentation.SECRET_VALUE));

        // Check secret value is not set to '*********'
        details = runOnServer.fetch(new TestComponents()).componentsDetailsMap();
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned2.getConfig().putSingle("secret", "updated secret value!!");
        components.component(id).update(returned2);

        // Check secret value is updated
        details = runOnServer.fetch(new TestComponents()).componentsDetailsMap();
        assertEquals("updated secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        ComponentRepresentation returned3 = components.query().stream().filter(c -> c.getId().equals(returned2.getId())).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned3.getConfig().getFirst("secret"));


        returned2.getConfig().putSingle("secret", "${vault.value}");
        components.component(id).update(returned2);

        // Check secret value is updated
        details = runOnServer.fetch(new TestComponents()).componentsDetailsMap();
        assertThat(details.get("mycomponent").getConfig().get("secret"), contains("${vault.value}"));

        ComponentRepresentation returned4 = components.query().stream().filter(c -> c.getId().equals(returned2.getId())).findFirst().get();
        assertThat(returned4.getConfig().get("secret"), contains("${vault.value}"));
    }

    @Test
    public void testCreateLongValue() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        final String randomLongString = RandomStringUtils.random(5000, true, true);

        rep.getConfig().putSingle("required", "Required");
        rep.getConfig().putSingle("val1", randomLongString);

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();

        assertThat(returned.getConfig().size(), equalTo(2));
        assertNotNull(returned.getConfig().getFirst("val1"));
        assertThat(returned.getConfig().getFirst("val1"), equalTo(randomLongString));
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

    private String createComponent(ComponentRepresentation rep) {
        return createComponent(managedRealm.admin(), rep);
    }

    private String createComponent(RealmResource realm, ComponentRepresentation rep) {
        ComponentsResource components = realm.components();
        Response response = components.add(rep);
        return ApiUtil.getCreatedId(response);
    }

    private void createComponentAndAssertError(ComponentRepresentation rep, String error) {
        ComponentsResource components = managedRealm.admin().components();
        Response response = components.add(rep);

        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
    }

    private ComponentRepresentation createComponentRepresentation(String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(managedRealm.getId());
        rep.setProviderId("test-component");
        rep.setProviderType(TestComponentProvider.class.getName());
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
            this(s, i, ComponentsTest.this.managedRealm.admin());
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

            ComponentResource c = managedRealm.admin().components().component(id);
            assertThat(c.toRepresentation(), Matchers.notNullValue());
            c.remove();

            log.debugf("Finished, id=%s", id);
        }
    }

    public static class TestComponents implements FetchOnServerWrapper<ComponentsDetails> {

        @Override
        public FetchOnServer getRunOnServer() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                return new ComponentsDetails(
                        realm.getComponentsStream(realm.getId(), TestComponentProvider.class.getName())
                                .collect(Collectors.toMap(ComponentModel::getName,
                                        componentModel -> {
                                            ProviderFactory<TestComponentProvider> f = session.getKeycloakSessionFactory()
                                                    .getProviderFactory(TestComponentProvider.class, componentModel.getProviderId());
                                            TestComponentProviderFactory factory = (TestComponentProviderFactory) f;
                                            TestComponentProvider p = (TestComponentProvider) factory.create(session, componentModel);
                                            return p.getDetails();
                                        }))
                );
            };
        }

        @Override
        public Class<ComponentsDetails> getResultClass() {
            return ComponentsDetails.class;
        }
    }

    /**
     * This class serves only as a wrapper for a {@code Map<String, TestProvider.DetailsRepresentation>}
     * This is crucial for deserialization to correctly reconstruct {@link TestComponentProvider.DetailsRepresentation}
     * objects, as {@link FetchOnServerWrapper#getResultClass()} cannot retain generic map type information at runtime.
     */
    private record ComponentsDetails(Map<String, TestComponentProvider.DetailsRepresentation> componentsDetailsMap) {
    }

    public static class ComponentsTestServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
