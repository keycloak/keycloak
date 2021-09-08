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

/**
 * Default Options for various authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class DefaultVirtualAuthOptions {

    public static VirtualAuthenticatorOptions DEFAULT = getDefault();

    // Default authenticators with different Transport type
    public static VirtualAuthenticatorOptions DEFAULT_BTE = getDefault().setTransport(VirtualAuthenticatorOptions.Transport.BLE);
    public static VirtualAuthenticatorOptions DEFAULT_NFC = getDefault().setTransport(VirtualAuthenticatorOptions.Transport.NFC);
    public static VirtualAuthenticatorOptions DEFAULT_USB = getDefault().setTransport(VirtualAuthenticatorOptions.Transport.USB);
    public static VirtualAuthenticatorOptions DEFAULT_INTERNAL = getDefault().setTransport(VirtualAuthenticatorOptions.Transport.INTERNAL);

    // Default authenticators with different Protocol type
    public static VirtualAuthenticatorOptions DEFAULT_CTAP_2 = getDefault().setProtocol(VirtualAuthenticatorOptions.Protocol.CTAP2);
    public static VirtualAuthenticatorOptions DEFAULT_U2F = getDefault().setProtocol(VirtualAuthenticatorOptions.Protocol.U2F);

    // YubiKey authenticators
    public static VirtualAuthenticatorOptions YUBIKEY_4 = getYubikeyGeneral();
    public static VirtualAuthenticatorOptions YUBIKEY_5_USB = getYubikeyGeneral();
    public static VirtualAuthenticatorOptions YUBIKEY_5_NFC = getYubikeyGeneral().setTransport(VirtualAuthenticatorOptions.Transport.NFC);

    private static VirtualAuthenticatorOptions getYubikeyGeneral() {
        return new VirtualAuthenticatorOptions()
                .setTransport(VirtualAuthenticatorOptions.Transport.USB)
                .setProtocol(VirtualAuthenticatorOptions.Protocol.U2F)
                .setHasUserVerification(true)
                .setIsUserConsenting(true)
                .setIsUserVerified(true);
    }

    private static VirtualAuthenticatorOptions getDefault() {
        return new VirtualAuthenticatorOptions();
    }
}
