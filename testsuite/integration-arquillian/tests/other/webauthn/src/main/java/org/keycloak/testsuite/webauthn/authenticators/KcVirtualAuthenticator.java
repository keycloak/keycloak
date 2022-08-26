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

import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.util.Arrays;
import java.util.Map;

/**
 * Keycloak Virtual Authenticator
 * <p>
 * Used as wrapper for VirtualAuthenticator and its options*
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class KcVirtualAuthenticator {
    private final VirtualAuthenticator authenticator;
    private final Options options;

    public KcVirtualAuthenticator(VirtualAuthenticator authenticator, VirtualAuthenticatorOptions options) {
        this.authenticator = authenticator;
        this.options = new Options(options);
    }

    public VirtualAuthenticator getAuthenticator() {
        return authenticator;
    }

    public Options getOptions() {
        return options;
    }

    public static final class Options {
        private final VirtualAuthenticatorOptions options;
        private final VirtualAuthenticatorOptions.Protocol protocol;
        private final VirtualAuthenticatorOptions.Transport transport;
        private final boolean hasResidentKey;
        private final boolean hasUserVerification;
        private final boolean isUserConsenting;
        private final boolean isUserVerified;
        private final Map<String, Object> map;

        private Options(VirtualAuthenticatorOptions options) {
            this.options = options;

            this.map = options.toMap();
            this.protocol = protocolFromMap(map);
            this.transport = transportFromMap(map);
            this.hasResidentKey = (Boolean) map.get("hasResidentKey");
            this.hasUserVerification = (Boolean) map.get("hasUserVerification");
            this.isUserConsenting = (Boolean) map.get("isUserConsenting");
            this.isUserVerified = (Boolean) map.get("isUserVerified");
        }

        public VirtualAuthenticatorOptions.Protocol getProtocol() {
            return protocol;
        }

        public VirtualAuthenticatorOptions.Transport getTransport() {
            return transport;
        }

        public boolean hasResidentKey() {
            return hasResidentKey;
        }

        public boolean hasUserVerification() {
            return hasUserVerification;
        }

        public boolean isUserConsenting() {
            return isUserConsenting;
        }

        public boolean isUserVerified() {
            return isUserVerified;
        }

        public VirtualAuthenticatorOptions clone() {
            return options;
        }

        public Map<String, Object> asMap() {
            return map;
        }

        private static VirtualAuthenticatorOptions.Protocol protocolFromMap(Map<String, Object> map) {
            return Arrays.stream(VirtualAuthenticatorOptions.Protocol.values())
                    .filter(f -> f.id.equals(map.get("protocol")))
                    .findFirst().orElse(null);
        }

        private static VirtualAuthenticatorOptions.Transport transportFromMap(Map<String, Object> map) {
            return Arrays.stream(VirtualAuthenticatorOptions.Transport.values())
                    .filter(f -> f.id.equals(map.get("transport")))
                    .findFirst().orElse(null);
        }
    }
}
