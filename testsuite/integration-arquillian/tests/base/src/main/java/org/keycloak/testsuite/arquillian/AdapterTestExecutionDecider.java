/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainers;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class AdapterTestExecutionDecider implements TestExecutionDecider {

    private final Logger log = Logger.getLogger(AdapterTestExecutionDecider.class);
    private static final Map<Method, CachedRecord> cache = new HashMap<>();

    @Inject private Instance<TestContext> testContextInstance;

    @Override
    public ExecutionDecision decide(Method method) {
        ExecutionDecision decision = getFromCache(method);
        if (decision != null) {
            return decision;
        }

        TestContext testContext = testContextInstance.get();
        if (!testContext.isAdapterTest()) {
            return execute(method, Boolean.TRUE, null);
        }
        if (testContext.isAdapterContainerEnabled() || testContext.isAdapterContainerEnabledCluster()) {

            if (method.isAnnotationPresent(AppServerContainer.class)) { // taking method level annotation first as it has higher priority
                if (getCorrespondingAnnotation(method).skip()) {
                    return execute(method, Boolean.FALSE, "Skipped by @AppServerContainer method level annotation.");
                }
            } else { //taking class level annotation
                if (getCorrespondingAnnotation(testContext.getTestClass()).skip()) {
                    return execute(method, Boolean.FALSE, "Skipped by @AppServerContainer class level annotation.");
                }
            }
            // execute otherwise
            return execute(method, Boolean.TRUE, null);

        } else {
            return execute(method, Boolean.FALSE, "Not enabled by @AppServerContainer annotations.");
        }
    }

    @Override
    public int precedence() {
        return 1;
    }

    private AppServerContainer getCorrespondingAnnotation(Method method) {

        AppServerContainers multipleAnnotations = method.getAnnotation(AppServerContainers.class);

        List<AppServerContainer> appServerContainers;
        if (multipleAnnotations != null) { // more than one @AppServerContainer annotation
            appServerContainers = Arrays.asList(multipleAnnotations.value());
        } else { // single @AppServerContainer annotation
            appServerContainers = Arrays.asList(method.getAnnotation(AppServerContainer.class));
        }

        return appServerContainers.stream()
                .filter(annotation -> annotation.value().equals(testContextInstance.get().getAppServerContainerName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not found the @AppServerContainer annotation with current app server."));
    }

    private AppServerContainer getCorrespondingAnnotation(Class testClass) {

        Class<?> annotatedClass = AppServerTestEnricher.getNearestSuperclassWithAppServerAnnotation(testClass);

        AppServerContainers multipleAnnotations = annotatedClass.getAnnotation(AppServerContainers.class);

        List<AppServerContainer> appServerContainers;
        if (multipleAnnotations != null) { // more than one @AppServerContainer annotation
            appServerContainers = Arrays.asList(multipleAnnotations.value());
        } else {// single @AppServerContainer annotation
            appServerContainers = Arrays.asList(annotatedClass.getAnnotation(AppServerContainer.class));
        }

        return appServerContainers.stream()
                .filter(annotation -> annotation.value().equals(testContextInstance.get().getAppServerContainerName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not found the @AppServerContainer annotation with current app server."));
    }

    private ExecutionDecision execute(Method method, Boolean execute, String message) {
        if (execute) {
            cache.put(method, new CachedRecord(Boolean.TRUE, ""));
            return ExecutionDecision.execute();
        } else {
            cache.put(method, new CachedRecord(Boolean.FALSE, message));
            log.debug(message);
            return ExecutionDecision.dontExecute(message);
        }
    }

    private ExecutionDecision getFromCache(Method method) {
        if (cache.containsKey(method)) {
            CachedRecord cachedRecord = cache.get(method);

            if (cachedRecord.execute) {
                return ExecutionDecision.execute(cachedRecord.message);
            } else {
                return ExecutionDecision.dontExecute(cachedRecord.message);
            }
        }
        return null;
    }

    private class CachedRecord {
        private final Boolean execute;
        private final String message;

        public CachedRecord(Boolean execute, String message) {
            this.execute = execute;
            this.message = message;
        }
    }
}
