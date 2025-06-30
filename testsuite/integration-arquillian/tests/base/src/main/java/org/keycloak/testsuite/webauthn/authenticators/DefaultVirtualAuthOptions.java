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

import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.util.function.Supplier;

import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Protocol.U2F;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport.BLE;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport.INTERNAL;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport.NFC;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport.USB;

/**
 * Default Options for various authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public enum DefaultVirtualAuthOptions {
    DEFAULT(VirtualAuthenticatorOptions::new),

    DEFAULT_BLE(() -> DEFAULT.getOptions().setTransport(BLE)),
    DEFAULT_NFC(() -> DEFAULT.getOptions().setTransport(NFC)),
    DEFAULT_USB(() -> DEFAULT.getOptions().setTransport(USB)),
    DEFAULT_INTERNAL(() -> DEFAULT.getOptions().setTransport(INTERNAL)),
    DEFAULT_RESIDENT_KEY(() -> DEFAULT.getOptions()
            .setHasResidentKey(true)
            .setHasUserVerification(true)
            .setIsUserVerified(true)
            .setIsUserConsenting(true)),

    YUBIKEY_4(DefaultVirtualAuthOptions::getYubiKeyGeneralOptions),
    YUBIKEY_5_USB(DefaultVirtualAuthOptions::getYubiKeyGeneralOptions),
    YUBIKEY_5_NFC(() -> getYubiKeyGeneralOptions().setTransport(NFC)),

    TOUCH_ID(() -> DEFAULT.getOptions()
            .setTransport(INTERNAL)
            .setHasUserVerification(true)
            .setIsUserVerified(true)
    );

    private final Supplier<VirtualAuthenticatorOptions> options;

    DefaultVirtualAuthOptions(Supplier<VirtualAuthenticatorOptions> options) {
        this.options = options;
    }

    public final VirtualAuthenticatorOptions getOptions() {
        return options.get();
    }

    private static VirtualAuthenticatorOptions getYubiKeyGeneralOptions() {
        return new VirtualAuthenticatorOptions()
                .setTransport(USB)
                .setProtocol(U2F)
                .setHasUserVerification(true)
                .setIsUserConsenting(true)
                .setIsUserVerified(true);
    }
}
