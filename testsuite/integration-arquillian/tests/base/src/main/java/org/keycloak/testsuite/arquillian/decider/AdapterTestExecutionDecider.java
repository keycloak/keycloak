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
package org.keycloak.testsuite.arquillian.decider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainers;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class AdapterTestExecutionDecider implements TestExecutionDecider {

    @Inject private Instance<TestContext> testContextInstance;

    @Override
    public ExecutionDecision decide(Method method) {
        TestContext testContext = testContextInstance.get();
        if (!testContext.isAdapterTest()) {
            return ExecutionDecision.execute();
        }
        if (testContext.isAdapterContainerEnabled() || testContext.isAdapterContainerEnabledCluster()) {

            // taking method level annotation first as it has higher priority
            if (method.isAnnotationPresent(AppServerContainers.class) || method.isAnnotationPresent(AppServerContainer.class)) {
                if (getCorrespondingAnnotation(method) == null) { //no corresponding annotation - taking class level annotation
                    if (getCorrespondingAnnotation(testContext.getTestClass()).skip()) {
                        return ExecutionDecision.dontExecute("Skipped by @AppServerContainer class level annotation.");
                    }
                } else if (getCorrespondingAnnotation(method).skip()) { //corresponding annotation
                    return ExecutionDecision.dontExecute("Skipped by @AppServerContainer method level annotation.");
                }
            } else { //taking class level annotation
                if (getCorrespondingAnnotation(testContext.getTestClass()) == null || 
                        getCorrespondingAnnotation(testContext.getTestClass()).skip()) {
                    return ExecutionDecision.dontExecute("Skipped by @AppServerContainer class level annotation.");
                }
            }
            // execute otherwise
            return ExecutionDecision.execute();
        }
        return ExecutionDecision.dontExecute("Not enabled by @AppServerContainer annotations.");
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
                .orElse(null);
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
                .orElse(null);
    }
}
