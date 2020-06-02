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
package org.keycloak.testsuite.arquillian.decider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

public class AuthServerExcludeExecutionDecider implements TestExecutionDecider {

    @Inject private Instance<TestContext> testContextInstance;

    @Override
    public int precedence() {
        return 4;
    }

    @Override
    public ExecutionDecision decide(Method method) {
        if (AppServerTestEnricher.isRemoteAppServer()) {
            return ExecutionDecision.execute();
        }
        TestContext testContext = testContextInstance.get();

        if (method.isAnnotationPresent(AuthServerContainerExclude.class)) {
            List<AuthServer> excluded = Arrays.asList(method.getAnnotation(AuthServerContainerExclude.class).value());
            
            if (AuthServerTestEnricher.isAuthServerRemote() && excluded.contains(AuthServer.REMOTE)) {
                return ExecutionDecision.dontExecute("Excluded by @AuthServerContainerExclude.");
            }

            if (AuthServerTestEnricher.isAuthServerQuarkus() && excluded.contains(AuthServer.QUARKUS)) {
                return ExecutionDecision.dontExecute("Excluded by @AuthServerContainerExclude.");
            }
        } else { //class
            if (testContext.getTestClass().isAnnotationPresent(AuthServerContainerExclude.class)) {
                List<AuthServer> excluded = Arrays.asList(((AuthServerContainerExclude) testContext.getTestClass().getAnnotation(AuthServerContainerExclude.class)).value());
                
                if (AuthServerTestEnricher.isAuthServerRemote() && excluded.contains(AuthServer.REMOTE)) {
                    return ExecutionDecision.dontExecute("Excluded by @AuthServerContainerExclude.");
                }

                if (AuthServerTestEnricher.isAuthServerQuarkus() && excluded.contains(AuthServer.QUARKUS)) {
                    return ExecutionDecision.dontExecute("Excluded by @AuthServerContainerExclude.");
                }
            }
        }

        return ExecutionDecision.execute(); //execute otherwise
    }
}
