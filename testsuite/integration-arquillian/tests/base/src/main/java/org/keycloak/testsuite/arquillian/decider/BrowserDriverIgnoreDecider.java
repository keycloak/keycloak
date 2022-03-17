/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDrivers;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverInstanceOf;

/**
 * Decider for ignoring tests for particular browsers (WebDrivers)
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class BrowserDriverIgnoreDecider implements TestExecutionDecider {

    @Inject
    private Instance<WebDriver> driver;

    @Inject
    private Instance<TestContext> testContextInstance;

    @Override
    public ExecutionDecision decide(Method method) {
        if (isAnnotationPresent(method)) {
            return decideIgnoring(method);
        } else { //class
            final TestContext testContext = testContextInstance.get();

            if (isAnnotationPresent(testContext.getTestClass())) {
                return decideIgnoring(testContext.getTestClass());
            }
        }
        return ExecutionDecision.execute();
    }

    private boolean isAnnotationPresent(AnnotatedElement element) {
        return element.isAnnotationPresent(IgnoreBrowserDriver.class) || element.isAnnotationPresent(IgnoreBrowserDrivers.class);
    }

    private ExecutionDecision decideIgnoring(AnnotatedElement element) {
        final WebDriver webDriver = driver.get();

        Predicate<IgnoreBrowserDriver> shouldBeIgnored = (item) -> {
            return webDriver != null && (isDriverInstanceOf(webDriver, item.value()) ^ item.negate());
        };

        return Arrays.stream(element.getAnnotationsByType(IgnoreBrowserDriver.class))
                .filter(shouldBeIgnored)
                .findAny()
                .map(f -> ExecutionDecision.dontExecute("This test should not be executed with this browser."))
                .orElse(ExecutionDecision.execute());
    }

    @Override
    public int precedence() {
        return Integer.MIN_VALUE;
    }
}
