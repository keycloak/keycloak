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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.idp.CreateIdentityProvider;
import org.keycloak.testsuite.console.page.idp.IdentityProvider;
import org.keycloak.testsuite.console.page.idp.IdentityProviders;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 *
 * @author Petr Mensik
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProviderTest extends AbstractConsoleTest {
    @Page
    private IdentityProviders identityProvidersPage;

    @Page
    private IdentityProvider identityProviderPage;

    @Page
    private CreateIdentityProvider createIdentityProviderPage;

    @Before
    public void beforeIdentityProviderTest() {
        identityProvidersPage.navigateTo();
    }

    @Test
    public void passwordMasking() {
        createIdentityProviderPage.setProviderId("google");
        identityProviderPage.setIds("google", "google");

        identityProvidersPage.addProvider("google");
        assertCurrentUrlEquals(createIdentityProviderPage);

        createIdentityProviderPage.form().setClientId("test-google");
        createIdentityProviderPage.form().setClientSecret("secret");
        assertEyeButtonIsEnabled();
        assertPasswordIsMasked();
        createIdentityProviderPage.form().clientSecret().clickEyeButton();
        assertPasswordIsUnmasked();
        createIdentityProviderPage.form().save();
        assertAlertSuccess();
        refreshPageAndWaitForLoad();
        assertCurrentUrlEquals(identityProviderPage);

        assertEyeButtonIsDisabled();
        assertPasswordIsMasked();
        identityProviderPage.form().setClientSecret("123456");
        assertEyeButtonIsEnabled();
        assertPasswordIsMasked();
        identityProviderPage.form().setClientSecret("${vault.fallout4}");
        assertEyeButtonIsDisabled();
        assertPasswordIsUnmasked();
        identityProviderPage.form().save();
        assertAlertSuccess();
        refreshPageAndWaitForLoad();
        assertCurrentUrlEquals(identityProviderPage);

        assertEyeButtonIsDisabled();
        assertPasswordIsUnmasked();
        identityProviderPage.form().setClientSecret("123456");
        assertEyeButtonIsEnabled();
        assertPasswordIsUnmasked();
        identityProviderPage.form().clientSecret().clickEyeButton();
        assertPasswordIsMasked();
    }

    private void assertEyeButtonIsDisabled() {
        assertTrue("Eye button is not disabled", identityProviderPage.form().clientSecret().isEyeButtonDisabled());
    }

    private void assertEyeButtonIsEnabled() {
        assertFalse("Eye button is not enabled", identityProviderPage.form().clientSecret().isEyeButtonDisabled());
    }

    private void assertPasswordIsMasked() {
        assertTrue("Password is not masked", identityProviderPage.form().clientSecret().isMasked());
    }

    private void assertPasswordIsUnmasked() {
        assertFalse("Password is not unmasked", identityProviderPage.form().clientSecret().isMasked());
    }
    
//    @Page
//    private IdentityProviderSettings idpSettingsPage;
//
////	@Test
//    public void testAddNewProvider() {
//        idpSettingsPage.addNewProvider(new Provider(SocialProvider.FACEBOOK, "klic", "secret"));
//        assertAlertSuccess();
//    }
//
////	@Test(expected = NoSuchElementException.class)
//    public void testDuplicitProvider() {
//        idpSettingsPage.addNewProvider(new Provider(SocialProvider.FACEBOOK, "a", "b"));
//    }
//
////	@Test
////    public void testEditProvider() {
////        page.goToPage(SETTINGS_SOCIAL);
////        page.editProvider(SocialProvider.FACEBOOK, new Provider(SocialProvider.FACEBOOK, "abc", "def"));
////    }
//
////	@Test
//    public void testDeleteProvider() {
//
//    }
//
//    @Test
//    @Ignore
//    public void testAddMultipleProviders() {
//    }
}
