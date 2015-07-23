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

package org.keycloak.testsuite.console.settings;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.model.PasswordPolicy;
import org.keycloak.testsuite.page.console.settings.CredentialsPage;

/**
 *
 * @author Petr Mensik
 */
public class CredentialsTest extends AbstractAdminConsoleTest<CredentialsPage> {
	
	@Before
	public void beforeCredentialsTest() {
		navigation.credentials();
	}
	
	@Test
        @Ignore("UI changes, see admin console: /#/realms/master/authentication/flows")
	public void testDigitsNumber() {
		page.addPolicy(PasswordPolicy.HASH_ITERATIONS, 5);
		page.removePolicy(PasswordPolicy.DIGITS);
	}
}
