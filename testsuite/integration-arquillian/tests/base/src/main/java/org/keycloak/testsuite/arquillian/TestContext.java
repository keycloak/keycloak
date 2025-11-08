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
package org.keycloak.testsuite.arquillian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.TestCleanup;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.getAppServerQualifiers;

/**
 *
 * @author tkyjovsk
 */
public final class TestContext {

    private final SuiteContext suiteContext;

    private final Class testClass;

    private ContainerInfo appServerInfo;
    private final List<ContainerInfo> appServerBackendsInfo = new ArrayList<>();

    private boolean adminLoggedIn;
    
    private Keycloak adminClient;
    private KeycloakTestingClient testingClient;
    private List<RealmRepresentation> testRealmReps = new ArrayList<>();
    private Map<String, String> userPasswords = new HashMap<>();

    // Track if particular test was initialized. What exactly means "initialized" is test dependent (Eg. some user in @Before method was created, so we can set initialized to true
    // to avoid creating user when @Before method is executed for 2nd time)
    private boolean initialized;

    // Key is realmName, value are objects to clean after the test method
    private final Map<String, TestCleanup> cleanups = new ConcurrentHashMap<>();

    private final Set<Runnable> afterClassActions = new HashSet<>();

    public TestContext(SuiteContext suiteContext, Class testClass) {
        this.suiteContext = suiteContext;
        this.testClass = testClass;
        this.adminLoggedIn = false;
    }

    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    public void setAdminLoggedIn(boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    public ContainerInfo getAppServerInfo() {
        return appServerInfo;
    }

    public void setAppServerInfo(ContainerInfo appServerInfo) {
        this.appServerInfo = appServerInfo;
    }

    public List<ContainerInfo> getAppServerBackendsInfo() {
        return appServerBackendsInfo;
    }
    
    public void setAppServerBackendsInfo(List<ContainerInfo> appServerBackendsInfo) {
        Collections.sort(appServerBackendsInfo);
        this.appServerBackendsInfo.addAll(appServerBackendsInfo);
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public void reconnectAdminClient() throws Exception {
        if (adminClient != null && !adminClient.isClosed()) {
            adminClient.close();
        }

        String authServerContextRoot = suiteContext.getAuthServerInfo().getContextRoot().toString();
        adminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), authServerContextRoot);
    }

    public boolean isAdapterTest() {
        return !getAppServerQualifiers(testClass).isEmpty();
    }

    public boolean isAdapterContainerEnabled() {
        if (!isAdapterTest()) return false; //no adapter test
        return getAppServerQualifiers(testClass).contains(ContainerConstants.APP_SERVER_PREFIX + AppServerTestEnricher.CURRENT_APP_SERVER);
    }

    public boolean isAdapterContainerEnabledCluster() {
        if (!isAdapterTest()) return false; //no adapter test
        if (appServerBackendsInfo.isEmpty()) return false; //no adapter clustered test
        
        Set<String> appServerQualifiers = getAppServerQualifiers(testClass);
        
        String qualifier = appServerBackendsInfo.stream()
                .map(ContainerInfo::getQualifier)
                .collect(Collectors.joining(";"));
        
        return appServerQualifiers.contains(qualifier);
    }
    
    public boolean isRelativeAdapterTest() {
        return isAdapterTest()
                && appServerInfo.getQualifier().equals(
                        suiteContext.getAuthServerInfo().getQualifier()); // app server == auth server
    }

    public SuiteContext getSuiteContext() {
        return suiteContext;
    }

    @Override
    public String toString() {
        return "TEST CONTEXT: " + getTestClass().getCanonicalName() + "\n"
                + (isAdapterTest() ? "Activated @AppServerContainer(" + getAppServerQualifiers(testClass) + ")\n" : "");
    }

    public Keycloak getAdminClient() {
        return adminClient;
    }

    public void setAdminClient(Keycloak adminClient) {
        this.adminClient = adminClient;
    }

    public KeycloakTestingClient getTestingClient() {
        if (testingClient == null) {
            String authServerContextRoot = suiteContext.getAuthServerInfo().getContextRoot().toString();
            testingClient = KeycloakTestingClient.getInstance(authServerContextRoot + "/auth");
        }
        return testingClient;
    }

    public void setTestingClient(KeycloakTestingClient testingClient) {
        this.testingClient = testingClient;
    }

    public List<RealmRepresentation> getTestRealmReps() {
        return testRealmReps;
    }

    public void setTestRealmReps(List<RealmRepresentation> testRealmReps) {
        this.testRealmReps = testRealmReps;
    }

    public void addTestRealmToTestRealmReps(RealmRepresentation testRealmRep) {
        this.testRealmReps.add(testRealmRep);
    }

    public void addTestRealmsToTestRealmReps(List<RealmRepresentation> testRealmReps) {
        this.testRealmReps.addAll(testRealmReps);
    }

    public Map<String, String> getUserPasswords() {
        return userPasswords;
    }

    public void setUserPasswords(Map<String, String> userPasswords) {
        this.userPasswords = userPasswords;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public TestCleanup getOrCreateCleanup(String realmName) {
        TestCleanup cleanup = cleanups.get(realmName);
        if (cleanup == null) {
            cleanup = new TestCleanup(this, realmName);
            TestCleanup existing = cleanups.putIfAbsent(realmName, cleanup);

            if (existing != null) {
                cleanup = existing;
            }
        }
        return cleanup;
    }

    public Map<String, TestCleanup> getCleanups() {
        return cleanups;
    }

    public void registerAfterClassAction(Runnable afterClassAction) {
        afterClassActions.add(afterClassAction);
    }

    public void runAfterClassActions() {
        afterClassActions.forEach(Runnable::run);
        afterClassActions.clear();
    }

    public String getAppServerContainerName() {
        if (isAdapterContainerEnabled()) { //standalone app server
            return getAppServerInfo().getArquillianContainer().getName();

        } else if (isAdapterContainerEnabledCluster()) { //clustered app server

            return getAppServerBackendsInfo().stream()
                .map(ContainerInfo::getQualifier)
                .collect(Collectors.joining(";"));
        }
        return null;
    }
}
