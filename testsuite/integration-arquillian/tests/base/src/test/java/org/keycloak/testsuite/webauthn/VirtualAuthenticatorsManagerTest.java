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
package org.keycloak.testsuite.webauthn;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.KcVirtualAuthenticator;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;

import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for VirtualAuthenticatorManager
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class VirtualAuthenticatorsManagerTest extends AbstractWebAuthnVirtualTest {

    @Drone
    @SecondBrowser
    WebDriver driver2;

    @Test
    public void addVirtualAuthenticator() {
        final VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(driver);
        assertThat(manager, notNullValue());

        KcVirtualAuthenticator authenticator = useDefaultTestingAuthenticator(manager);
        assertAuthenticatorOptions(authenticator);

        manager.removeAuthenticator();
        assertThat(manager.getCurrent(), Matchers.nullValue());

        authenticator = useDefaultTestingAuthenticator(manager);
        assertAuthenticatorOptions(authenticator);

        manager.removeAuthenticator();
        assertThat(manager.getCurrent(), Matchers.nullValue());
    }

    @Test
    public void overrideUsedAuthenticator() {
        final VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(driver);
        assertThat(manager, notNullValue());

        KcVirtualAuthenticator defaultTesting = useDefaultTestingAuthenticator(manager);
        assertAuthenticatorOptions(defaultTesting);
        assertThat(manager.getCurrent(), is(defaultTesting));

        VirtualAuthenticatorOptions defaultBleOptions = DefaultVirtualAuthOptions.DEFAULT_BLE.getOptions();
        assertThat(defaultBleOptions, notNullValue());

        KcVirtualAuthenticator defaultBLE = manager.useAuthenticator(defaultBleOptions);
        assertThat(defaultBLE, notNullValue());
        assertAuthenticatorOptions(defaultTesting);

        assertThat(manager.getCurrent(), is(defaultBLE));
        assertThat(manager.getCurrent().getOptions().clone(), is(defaultBleOptions));
    }

    @Test
    public void differentDriver() {
        final VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(driver);
        assertThat(manager, notNullValue());

        KcVirtualAuthenticator authenticator = useDefaultTestingAuthenticator(manager);
        assertThat(authenticator, notNullValue());
        assertThat(manager.getCurrent(), notNullValue());

        final VirtualAuthenticatorManager manager2 = new VirtualAuthenticatorManager(driver2);
        assertThat(manager2, notNullValue());
        assertThat(manager2.getCurrent(), nullValue());
    }

    @Test
    public void singleResponsibleAuthOptions() {
        VirtualAuthenticatorOptions options = DefaultVirtualAuthOptions.DEFAULT_BLE.getOptions();
        options.setTransport(VirtualAuthenticatorOptions.Transport.NFC);

        final VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(driver);
        assertThat(manager, notNullValue());

        manager.useAuthenticator(options);

        assertThat(manager.getCurrent().getOptions().getTransport(), is(VirtualAuthenticatorOptions.Transport.NFC));

        options = DefaultVirtualAuthOptions.DEFAULT_BLE.getOptions();
        manager.useAuthenticator(options);

        assertThat(manager.getCurrent().getOptions().getTransport(), is(VirtualAuthenticatorOptions.Transport.BLE));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    private static KcVirtualAuthenticator useDefaultTestingAuthenticator(VirtualAuthenticatorManager manager) {
        KcVirtualAuthenticator authenticator = manager.useAuthenticator(defaultTestingAuthenticatorOptions());
        assertThat(authenticator, notNullValue());

        assertThat(manager.getCurrent(), is(authenticator));

        return authenticator;
    }

    private static void assertAuthenticatorOptions(KcVirtualAuthenticator authenticator) {
        KcVirtualAuthenticator.Options options = authenticator.getOptions();
        assertThat(options, notNullValue());
        assertThat(options.getProtocol(), is(VirtualAuthenticatorOptions.Protocol.CTAP2));
        assertThat(options.getTransport(), is(VirtualAuthenticatorOptions.Transport.BLE));
        assertThat(options.hasUserVerification(), is(true));
        assertThat(options.isUserConsenting(), is(false));
        assertThat(options.hasResidentKey(), is(true));
        assertThat(options.isUserVerified(), is(true));
    }

    private static VirtualAuthenticatorOptions defaultTestingAuthenticatorOptions() {
        return new VirtualAuthenticatorOptions()
                .setProtocol(VirtualAuthenticatorOptions.Protocol.CTAP2)
                .setTransport(VirtualAuthenticatorOptions.Transport.BLE)
                .setHasUserVerification(true)
                .setIsUserConsenting(false)
                .setHasResidentKey(true)
                .setIsUserVerified(true);
    }
}
