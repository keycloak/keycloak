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
package org.keycloak.testsuite.console.idp;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.page.idp.IdentityProviderSettings;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.model.Provider;
import org.keycloak.testsuite.model.SocialProvider;

/**
 *
 * @author Petr Mensik
 */
public class IdentityProviderTest extends AbstractConsoleTest {
    
    @Page
    private IdentityProviderSettings idpSettingsPage;

//	@Test
    public void testAddNewProvider() {
        idpSettingsPage.addNewProvider(new Provider(SocialProvider.FACEBOOK, "klic", "secret"));
        assertAlertSuccess();
    }

//	@Test(expected = NoSuchElementException.class)
    public void testDuplicitProvider() {
        idpSettingsPage.addNewProvider(new Provider(SocialProvider.FACEBOOK, "a", "b"));
    }

//	@Test
//    public void testEditProvider() {
//        page.goToPage(SETTINGS_SOCIAL);
//        page.editProvider(SocialProvider.FACEBOOK, new Provider(SocialProvider.FACEBOOK, "abc", "def"));
//    }

//	@Test
    public void testDeleteProvider() {

    }

    @Test
    @Ignore
    public void testAddMultipleProviders() {
    }
}
