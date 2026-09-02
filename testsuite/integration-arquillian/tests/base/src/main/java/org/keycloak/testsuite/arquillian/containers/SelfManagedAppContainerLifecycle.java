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
package org.keycloak.testsuite.arquillian.containers;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.operations.OperationException;

/**
 * The test implementing the interface is expected to maintain container lifecycle 
 * itself. No app server container will be started.
 * 
 * @author vramik
 */
public interface SelfManagedAppContainerLifecycle {

    /**
     * Should be called @Before
     */
    void startServer() throws InterruptedException, IOException, OperationException, TimeoutException, CommandFailedException, CliException;

    /**
     * Should be called @After
     */
    void stopServer();
}
