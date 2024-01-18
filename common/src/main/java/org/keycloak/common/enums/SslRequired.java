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

import org.keycloak.common.ClientConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public boolean isRequired(String address) {
        switch (this) {
            case ALL:
                return true;
            case NONE:
                return false;
            case EXTERNAL:
                return !isLocal(address);
            default:
                return true;
        }
    }

    private boolean isLocal(String remoteAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(remoteAddress);

            // Check if the IP address is any local address, loopback address, or site-local address
            boolean isLocal = inetAddress.isAnyLocalAddress() ||
                              inetAddress.isLoopbackAddress() ||
                              inetAddress.isSiteLocalAddress();

            // Additionally, check if the IP address is in the 100.64.0.0/10 range
            boolean isSpecialRange = isInSpecialRange(inetAddress);

            return isLocal || isSpecialRange;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isInSpecialRange(InetAddress inetAddress) {
        // Check if the IP address falls within the 100.64.0.0/10 range
        byte[] addressBytes = inetAddress.getAddress();
        return (addressBytes[0] & 0xFF) == 100 && (addressBytes[1] & 0xC0) == 0x40;
    }

}
