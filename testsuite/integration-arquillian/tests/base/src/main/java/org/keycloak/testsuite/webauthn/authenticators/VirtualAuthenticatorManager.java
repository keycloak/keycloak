/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.authenticators;

import org.hamcrest.CoreMatchers;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Manager for Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class VirtualAuthenticatorManager {
    private final HasVirtualAuthenticator driver;
    private KcVirtualAuthenticator currentAuthenticator;

    public VirtualAuthenticatorManager(WebDriver driver) {
        assertThat("Driver must support Virtual Authenticators", driver, CoreMatchers.instanceOf(HasVirtualAuthenticator.class));
        this.driver = (HasVirtualAuthenticator) driver;
    }

    public KcVirtualAuthenticator useAuthenticator(VirtualAuthenticatorOptions options) {
        if (options == null) return null;

        removeAuthenticator();
        this.currentAuthenticator = new KcVirtualAuthenticator(driver.addVirtualAuthenticator(options), options);
        return currentAuthenticator;
    }

    public KcVirtualAuthenticator getCurrent() {
        return currentAuthenticator;
    }

    public void removeAuthenticator() {
        if (currentAuthenticator != null) {
            currentAuthenticator.getAuthenticator().removeAllCredentials();
            driver.removeVirtualAuthenticator(currentAuthenticator.getAuthenticator());
            this.currentAuthenticator = null;
        }
    }
}
