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
package org.keycloak.testsuite;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author Petr Mensik
 */
@RunWith(Arquillian.class)
public abstract class AbstractTest extends KeyCloakServer {

	protected static String LOGIN_URL = "";

	@Drone
	protected WebDriver driver;

	@Before
	public void beforeAllTest() {
		if (!isServerRunning()) {
//		if(!KeyCloakServer.RUNNING) {
			startServer();
		}
	}

//	@AfterClass
	public static void afterClass() {
		KeyCloakServer.RUNNING = false;
	}

}
