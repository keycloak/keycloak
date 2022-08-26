/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.test.impl.execution.LocalTestExecuter;
import org.jboss.arquillian.container.test.impl.execution.event.LocalExecutionEvent;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestResult;
import org.keycloak.common.Profile;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelTestExecutor extends LocalTestExecuter {

    @Inject
    private Instance<TestContext> testContext;

    @Override
    public void execute(LocalExecutionEvent event) throws Exception {
        Method testMethod = event.getExecutor().getMethod();

        ModelTest annotation = testMethod.getAnnotation(ModelTest.class);

        if (annotation == null) {
            // Not a model test
            super.execute(event);
        } else {
            TestResult result = new TestResult();
            if (annotation.skipForMapStorage() && Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE)) {
                result = TestResult.skipped();
            }
            else {
                try {
                    // Model test - wrap the call inside the
                    TestContext ctx = testContext.get();
                    KeycloakTestingClient testingClient = ctx.getTestingClient();
                    testingClient.server().runModelTest(testMethod.getDeclaringClass().getName(), testMethod.getName());

                    result.setStatus(TestResult.Status.PASSED);
                } catch (Throwable e) {
                    result.setStatus(TestResult.Status.FAILED);
                    result.setThrowable(e);
                } finally {
                    result.setEnd(System.currentTimeMillis());
                }
            }

            // Need to use reflection this way...
            Field testResultField = Reflections.findDeclaredField(LocalTestExecuter.class, "testResult");
            testResultField.setAccessible(true);
            InstanceProducer<TestResult> thisTestResult = (InstanceProducer<TestResult>) testResultField.get(this);

            thisTestResult.set(result);
        }
    }
}
