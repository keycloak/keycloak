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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DisableAuthorizationSettingsTest extends AbstractAuthorizationSettingsTest {

    @Test
    public void testDisableAuthorization() throws InterruptedException {
        clientSettingsPage.navigateTo();
        clientSettingsPage.form().setAuthorizationSettingsEnabled(false);
        clientSettingsPage.form().confirmDisableAuthorizationSettings();
        Thread.sleep(1000);
        clientSettingsPage.form().save();
        assertAlertSuccess();

        clientSettingsPage.navigateTo();
        assertFalse(clientSettingsPage.form().isAuthorizationSettingsEnabled());
    }
}
