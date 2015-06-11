/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.adapter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.AbstractTest;
import org.keycloak.testsuite.adapter.servlet.InputServlet;
import org.keycloak.testsuite.adapter.servlet.SessionServlet;

import static org.junit.Assert.*;
import org.keycloak.testsuite.adapter.servlet.CustomerServlet;
import org.keycloak.testsuite.ui.application.page.InputPage;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class AdapterTest extends AbstractTest {

	@Page
	private InputPage inputPage;

	@Test
	@RunAsClient
	public void test() throws InterruptedException {
		deployApplication(SessionServlet.class);
		Thread.sleep(20_000);
	}

	public void testSavedPostRequest() {
		deployApplication(InputServlet.class);
		String inputAppURL = getStringApplicationURL() + "/input-portal";
		driver.get(inputAppURL);
		assertTrue(driver.getCurrentUrl().contains(inputAppURL));
		inputPage.execute("hello");

		Client client = ClientBuilder.newClient();
		Form form = new Form();
		form.param("parameter", "hello");
		String text = client.target(inputAppURL + "/unsecured").request().post(Entity.form(form), String.class);
		assertTrue(text.contains("parameter=hello"));
		client.close();
	}

	public void testServletRequestLogout() {
		deployApplication(CustomerServlet.class);
		String customerAppURL = getStringApplicationURL() + "/customer-portal";
		driver.getCurrentUrl().startsWith(LOGIN_URL);
	}

}
