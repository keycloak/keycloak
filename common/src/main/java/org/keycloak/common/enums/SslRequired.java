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

package org.keycloak.common.enums;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.keycloak.common.ClientConnection;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum SslRequired {

    ALL,
    EXTERNAL,
    NONE;

    public boolean isRequired(ClientConnection connection) {
        return isRequired(connection.getRemoteAddr());
    }

    public boolean isRequired(String host) {
        switch (this) {
            case ALL:
                return true;
            case NONE:
                return false;
            case EXTERNAL:
                // NOTE: this is sometimes using hostnames here, which require DNS resolution
                // It assumes that the resolution will be the same on the client side
                // - this will go away once EXTERNAL is no longer supported
                return !isLocal(host);
            default:
                return true;
        }
    }

    private boolean isLocal(String host) {
        if (host == null || host.isEmpty()) {
            return false; // InetAddress.getByName returns localhost for these
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress() || inetAddress.isLinkLocalAddress() || isUniqueLocal(inetAddress);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Check if the address is within IPv6 unique local address (ULA) range RFC4193.
     */
    private boolean isUniqueLocal(InetAddress address) {
        if (address instanceof java.net.Inet6Address) {
            byte[] addr = address.getAddress();
            // Check if address is in unique local range fc00::/7
            return ((byte) (addr[0] & 0b11111110)) == (byte) 0xFC;
        }

        return false;
    }

}
