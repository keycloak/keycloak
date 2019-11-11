/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;

/**
 * A {@link TestExecutionDecider} that skips tests annotated with {@link EnableVault} with the Elytron credential store
 * provider on Undertow as this particular provider is available as a WildFly extension only.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class VaultTestExecutionDecider implements TestExecutionDecider {

    @Inject
    private Instance<TestContext> testContextInstance;

    @Override
    public ExecutionDecision decide(Method method) {
        TestContext testContext = testContextInstance.get();
        // if test was annotated with EnableVault, check if it has selected the elytron credential store provider.
        if (testContext.getTestClass().isAnnotationPresent(EnableVault.class)) {
            EnableVault.PROVIDER_ID providerId = testContext.getTestClass().getAnnotation(EnableVault.class).providerId();
            if (providerId == EnableVault.PROVIDER_ID.ELYTRON_CS_KEYSTORE) {
                // if the auth server is undertow, skip the test.
                SuiteContext suiteContext = testContext.getSuiteContext();
                if (suiteContext != null && suiteContext.getAuthServerInfo() != null && suiteContext.getAuthServerInfo().isUndertow()) {
                    return ExecutionDecision.dontExecute("@EnableVault with Elytron credential store provider not supported on Undertow, skipping");
                }
            }
        }
        return ExecutionDecision.execute();
    }

    @Override
    public int precedence() {
        return 3;
    }
}
