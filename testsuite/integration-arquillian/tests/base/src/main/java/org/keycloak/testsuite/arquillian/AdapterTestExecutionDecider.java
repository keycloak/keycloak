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
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class AdapterTestExecutionDecider implements TestExecutionDecider {

    private final Logger log = Logger.getLogger(AdapterTestExecutionDecider.class);

    @Inject private Instance<TestContext> testContextInstance;

    @Override
    public ExecutionDecision decide(Method method) {
        TestContext testContext = testContextInstance.get();
        if (!testContext.isAdapterTest()) return ExecutionDecision.execute();
        if (testContext.isAdapterContainerEnabled() || testContext.isAdapterContainerEnabledCluster()) {
            return ExecutionDecision.execute();
        } else {
            log.debug("Skipping test: Not enabled by @AppServerContainer annotations.");
            return ExecutionDecision.dontExecute("Not enabled by @AppServerContainer annotations.");
        }
    }

    @Override
    public int precedence() {
        return 1;
    }

}
