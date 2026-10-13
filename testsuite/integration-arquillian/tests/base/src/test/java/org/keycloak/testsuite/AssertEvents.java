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

package org.keycloak.testsuite;

import org.keycloak.representations.idm.EventRepresentation;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AssertEvents implements TestRule {

    private final AbstractKeycloakTest context;
    private int skipEvents = 0;

    public AssertEvents(AbstractKeycloakTest ctx) {
        context = ctx;
    }

    @Override
    public Statement apply(final Statement base, org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // TODO: Ideally clear the queue just before testClass rather then before each method
                clear();
                base.evaluate();
                // TODO Test should fail if there are leftover events
            }
        };
    }

    public EventRepresentation poll() {
        while(skipEvents > 0) {
            if (context.testingClient.testing().pollEvent() == null) {
                return null;
            }
            skipEvents--;
        }
        return context.testingClient.testing().pollEvent();
    }

    public void skip(int skipNumOfEvents) {
        skipEvents += skipNumOfEvents;
    }

    public void clear() {
        context.getTestingClient().testing().clearEventQueue();
    }
}
