/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.ui;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;

/**
 *
 * @author Petr Mensik
 */
public abstract class KeyCloakServer {
	
	public static boolean RUNNING = false;
	
	private static final String CONTAINER_NAME = System.getProperty("container-name");
	
	@ArquillianResource
	private ContainerController controller;
	
	@ArquillianResource
	private Deployer deployer;
	
	public void startServer(String containerName) {
		controller.start(containerName);
	}
	
	public void startServer() {
		startServer(CONTAINER_NAME);
	}
	
	public void stopServer(String containerName) {
		controller.stop(containerName);
	}
	
	public void stopServer() {
		stopServer(CONTAINER_NAME);
	}
	
	public String getContainerName() {
		return CONTAINER_NAME;
	}
}
