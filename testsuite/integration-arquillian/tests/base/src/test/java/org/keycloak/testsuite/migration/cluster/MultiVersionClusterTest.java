/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.migration.cluster;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import org.infinispan.Cache;
import org.infinispan.util.concurrent.TimeoutException;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.infinispan.WrapperClusterEvent;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.cluster.AbstractClusterTest;
import static org.keycloak.testsuite.arquillian.containers.KeycloakContainerEventsController.deploy;
import static org.keycloak.testsuite.arquillian.containers.KeycloakContainerEventsController.undeploy;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rest.TestClassLoader;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.runonserver.SerializationUtil;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.OAuthClient;

public class MultiVersionClusterTest extends AbstractClusterTest {

    private static ContainerInfo currentNode;
    private static ContainerInfo legacyNode;
    private static boolean initialized = false;

    @Page
    protected LoginPage loginPage;

    static class CacheValuesHolder {
        private Map<String, Map<String, Object>> values;

        public CacheValuesHolder() {
        }

        public CacheValuesHolder(final Map<String, Map<String, Object>> values) {
            this.values = values;
        }

        public Map<String, Map<String, Object>> getValues() {
            return values;
        }

        public void setValues(Map<String, Map<String, Object>> values) {
            this.values = values;
        }
    }

    @BeforeClass
    public static void enabled() {
        Assume.assumeThat(System.getProperty("auth.server.legacy.version"), notNullValue());
    }

    @Before
    @Override
    public void beforeClusterTest() {
        if (!initialized) {
            currentNode = backendNode(0);
            legacyNode = suiteContext.getLegacyAuthServerInfo();
            addAdminJsonFileToLegacy();

            initialized = true;
        }
        startBackendNode(legacyNode);
        startBackendNode(currentNode);
    }
    
    @After
    public void after() {
        killBackendNode(legacyNode);
        killBackendNode(currentNode);
    }

    private JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "negative.jar")
                .addPackage("org/keycloak/testsuite")
                .addClass(SerializableTestClass.class);
    }

    @Test
    public void verifyFailureOnLegacy() throws Exception {

        deploy(deployment(), currentNode);
        
        try {
            backendTestingClients.get(currentNode).server().run(session -> {
                try {
                    Class<?> itShouldFail = Module.getContextModuleLoader().loadModule("deployment.negative.jar").getClassLoader()
                            .loadClassLocal(SerializableTestClass.class.getName());
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME)
                            .put("itShouldFail", Reflections.newInstance(itShouldFail));
                } catch (Exception ex) {
                    throw new RunOnServerException(ex);
                }
            });
        } catch (Exception e) {
            assertThat(e, instanceOf(RunOnServerException.class));
            assertThat(e.getCause().getCause(), instanceOf(TimeoutException.class));
        } finally {
            undeploy(deployment(), currentNode);
        }
    }

    @Test
    public void verifyFailureOnCurrent() throws Exception {

        deploy(deployment(), legacyNode);
        
        try {
            backendTestingClients.get(legacyNode).server().run(session -> {
                try {
                    Class<?> itShouldFail = Module.getContextModuleLoader().loadModule("deployment.negative.jar").getClassLoader()
                            .loadClassLocal(SerializableTestClass.class.getName());
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME)
                            .put("itShouldFail", Reflections.newInstance(itShouldFail));
                } catch (Exception ex) {
                    throw new RunOnServerException(ex);
                }
            });
        } catch (Exception e) {
            assertThat(e, instanceOf(RunOnServerException.class));
            assertThat(e.getCause().getCause(), instanceOf(TimeoutException.class));
        } finally {
            undeploy(deployment(), legacyNode);
        }
    }

    /*
     * Tests if legacy node remains usable (login) after current node connects to cluster
     */
    @Test
    public void loginSuccessToLegacy() throws Exception {
        String originalServerRoot = OAuthClient.SERVER_ROOT;
        try {
            OAuthClient.updateURLs(legacyNode.getContextRoot().toString());
            OAuthClient oauth = new OAuthClient();
            oauth.init(DroneUtils.getCurrentDriver());
            oauth.realm(MASTER).clientId("account").redirectUri(legacyNode.getContextRoot().toString() + "/auth/realms/master/account/");
            
            oauth.openLoginForm();
            assertThat(DroneUtils.getCurrentDriver().getTitle(), containsString("Sign in to "));
            loginPage.login("admin", "admin");

            assertThat("Login was not successful.", oauth.getCurrentQuery().get(OAuth2Constants.CODE), notNullValue());
        } finally {
            OAuthClient.updateURLs(originalServerRoot);
        }
    }

    @Test
    public void fromLegacyToCurrent() {
        Map<String, Map<String, Object>> expected = createCacheAndGetFromServer(legacyNode);
        Map<String, Map<String, Object>> actual = getFromServer(currentNode, SerializationUtil.encode(expected.keySet().toString()));
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void fromCurrentToLegacy() {
        Map<String, Map<String, Object>> expected = createCacheAndGetFromServer(currentNode);
        Map<String, Map<String, Object>> actual = getFromServer(legacyNode, SerializationUtil.encode(expected.keySet().toString()));
        assertThat(actual, equalTo(expected));
    }

    private void addAdminJsonFileToLegacy() {
        try {
            FileUtils.copyFile(new File("target/test-classes/keycloak-add-user.json"), 
                new File(System.getProperty("auth.server.legacy.home")
                + "/standalone/configuration/keycloak-add-user.json"));
            log.debug("Successfully added keycloak-add-user.json to " + System.getProperty("auth.server.legacy.home")
                + "/standalone/configuration/keycloak-add-user.json");
        } catch (IOException ex) {
            throw new RuntimeException("Adding admin json file failed.", ex);
        }
    }

    private Map<String, Map<String, Object>> createCacheAndGetFromServer(ContainerInfo container) {
        return backendTestingClients.get(container).server().fetch(session -> {
            Map<String, Map<String, Object>> result = new HashMap<>();               

            try {
                Indexer indexer = new Indexer();
                DotName serializeWith = DotName.createSimple("org.infinispan.commons.marshall.SerializeWith");

                ModuleLoader contextModuleLoader = Module.getContextModuleLoader();
                Module module = contextModuleLoader.loadModule("org.keycloak.keycloak-model-infinispan");
                ModuleClassLoader classLoader = module.getClassLoader();

                Enumeration<URL> resources = classLoader.getResources("org/keycloak");
                while (resources.hasMoreElements()) {
                    URL nextElement = resources.nextElement();
                    Enumeration<JarEntry> entries = new JarFile(nextElement.getFile().replace("file:", "").replace("!/org/keycloak", "")).entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            indexer.index(classLoader.getResourceAsStream(entry.getName()));
                        }
                    }
                }

                Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

                for (AnnotationInstance annotation : indexer.complete().getAnnotations(serializeWith)) {

                    switch (annotation.target().kind()) {
                        case CLASS:
                            Map<String, Object> fieldValue = new HashMap<>();
                            String className = annotation.target().asClass().name().toString();
                            Class<Serializable> classForName = Reflections.classForName(className, classLoader);
                            Object newInstance;

                            if (Arrays.asList(classForName.getDeclaredConstructors()).stream()
                                    .filter(c -> !c.isSynthetic())
                                    .anyMatch(c -> c.getParameterTypes().length == 0 )) {
                                newInstance = Reflections.newInstance(classForName);
                            } else {
                                Constructor<?> constructor = Arrays.asList(classForName.getDeclaredConstructors()).stream()
                                    .filter(c -> !c.isSynthetic())
                                    .findFirst().get();
                                constructor.setAccessible(true);

                                List<Object> parameters = new ArrayList<>();
                                for (Class<?> type : constructor.getParameterTypes()) {
                                    if (type.isPrimitive()) { // we have to set all primitive values in constructor
                                        if (type.equals(Boolean.TYPE)) {
                                            parameters.add(false);
                                        } else if (type.equals(Character.TYPE)) {
                                            parameters.add(' ');
                                        } else {
                                            parameters.add(0);
                                        }
                                    } else if (type.equals(UUID.class)) { //UUID cannot be null
                                        parameters.add(UUID.randomUUID());
                                    } else {
                                        parameters.add(null); // all fields will be set in next step
                                    }
                                }
                                newInstance = constructor.newInstance(parameters.toArray());
                            }

                            Set<Field> fields = Reflections.getAllDeclaredFields(classForName).stream()
                                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                                    .collect(Collectors.toSet());

                            for (Field field : fields) {
                                field.setAccessible(true);
                                Class<?> type = field.getType();
                                Object value;
                                if (type.equals(KeycloakSession.class)) {
                                    value = session;
                                } else if (type.equals(String.class)) {
                                    value = UUID.randomUUID().toString();
                                } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
                                    value = Boolean.FALSE;
                                } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
                                    value = new Random().nextInt();
                                } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
                                    value = new Random().nextLong();
                                } else if (type.equals(AuthenticatedClientSessionStore.class)) {
                                    value = new AuthenticatedClientSessionStore();
                                } else if (type.equals(UserSessionModel.State.class)) {
                                    value = UserSessionModel.State.LOGGING_OUT;
                                } else if (type.equals(Map.class)) {
                                    value = new HashMap();
                                } else if (type.equals(ConcurrentHashMap.class)) {
                                    value = new ConcurrentHashMap();
                                } else if (type.equals(Set.class)) {
                                    value = new HashSet();
                                } else if (type.equals(ClusterEvent.class)) {
                                    value = new WrapperClusterEvent();
                                } else if (type.equals(UUID.class)) {
                                    value = UUID.randomUUID();
                                } else if (type.equals(SessionEntity.class)) {
                                    value = new UserSessionEntity();
                                } else if (type.equals(BitSet.class)) {
                                    value = new BitSet();
                                } else {
                                    throw new IllegalStateException(className + " - Uncovered parameter type: " + type);
                                }
                                field.set(newInstance, value);
                                fieldValue.put(field.getName(), value);
                            }

                            cache.put(className, newInstance);
                            result.put(className, fieldValue);
                            break;
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return new CacheValuesHolder(result);
        }, CacheValuesHolder.class).getValues();
    }

    private Map<String, Map<String, Object>> getFromServer(ContainerInfo container, final String classes) {
        return backendTestingClients.get(container).server().fetch(session -> {

            Map<String, Map<String, Object>> mapa = new HashMap<>();
            Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

            String decoded = (String) SerializationUtil.decode(classes, TestClassLoader.getInstance());
            for (String className : decoded.replace("[", "").replace("]", "").split(", ")) {
                Map<String, Object> fieldValues = new HashMap<>();
                Object cacheEntry = cache.get(className);
                Reflections.getAllDeclaredFields(cacheEntry.getClass()).stream()
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .forEach(field -> {
                        field.setAccessible(true);
                        Object fieldValue = Reflections.getFieldValue(field, cacheEntry);
                        fieldValues.put(field.getName(), fieldValue);
                    });
                mapa.put(className, fieldValues);
            }
            return new CacheValuesHolder(mapa);
        }, CacheValuesHolder.class).getValues();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }
}
