/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.util;

import org.keycloak.testsuite.AbstractKeycloakTest;

import org.jboss.logging.Logger;
import org.junit.rules.ExternalResource;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanTestTimeServiceRule extends ExternalResource {

    private static final Logger log = Logger.getLogger(InfinispanTestTimeServiceRule.class);

    private final AbstractKeycloakTest test;

    public InfinispanTestTimeServiceRule(AbstractKeycloakTest test) {
        this.test = test;
    }

    @Override
    protected void before() throws Throwable {
        test.getTestingClient().testing().setTestingInfinispanTimeService();
    }

    @Override
    protected void after() {
        test.getTestingClient().testing().revertTestingInfinispanTimeService();
    }
}
