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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private final Map customContext = new HashMap<>();

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

    public Class getTestClass() {
        return testClass;
    }

    public boolean isAdapterTest() {
        return appServerInfo != null;
    }

    public boolean isRelativeAdapterTest() {
        return isAdapterTest()
                && appServerInfo.getQualifier().equals(
                        suiteContext.getAuthServerInfo().getQualifier()); // app server == auth server
    }

    public boolean isClusteredAdapterTest() {
        return isAdapterTest() && !appServerBackendsInfo.isEmpty();
    }

    public SuiteContext getSuiteContext() {
        return suiteContext;
    }

    @Override
    public String toString() {
        return "TEST CONTEXT: " + getTestClass().getCanonicalName() + "\n"
                + (isAdapterTest() ? "App server container: " + getAppServerInfo() + "\n" : "");
    }

    public Object getCustomValue(Object key) {
        return customContext.get(key);
    }
    
    public void setCustomValue(Object key, Object value) {
        customContext.put(key, value);
    }

}
