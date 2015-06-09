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
package org.keycloak.testsuite.integration.ui;

import java.net.URL;
import javax.servlet.Servlet;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 * @author Petr Mensik
 */
	public abstract class KeyCloakServer {

		public static boolean RUNNING = false;
		
		private static final String CONTAINER_NAME = System.getProperty("container-name");
		private static final String DEPLOYMENT_NAME = "deployment";

		private static final WebArchive deployment = ShrinkWrap.create(WebArchive.class, "application.war");

		@ArquillianResource
		private ContainerController controller;

		@ArquillianResource
		private Deployer deployer;
		
//		@ArquillianResource
		private URL url;

		@TargetsContainer(value = "wildfly-8-managed")
//		@OverProtocol("Servlet 3.0")
		@Deployment(name = DEPLOYMENT_NAME, managed = false)//, testable = false)
		private static WebArchive createDeployment() {
			return deployment;
		}

		public void deployApplication(Class<? extends Servlet>... servlets) {
	//		deployment.delete(new BasicPath());
			for (Class<? extends Servlet> servlet : servlets) {
				deployment.addClass(servlet);
			}
			deployer.deploy(DEPLOYMENT_NAME);
		}

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
		
		public boolean isServerRunning() {
			return isServerRunning(CONTAINER_NAME);
		}
		
		public boolean isServerRunning(String containerName) {
			return controller.isStarted(containerName);
		}

		public String getContainerName() {
			return CONTAINER_NAME;
		}
		
		public URL getApplicationURL() {
			return url;
		}
		
		public String getStringApplicationURL() {
			return url.toExternalForm();
		}
	}