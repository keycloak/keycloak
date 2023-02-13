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

package org.keycloak.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HostUtils {

    // Best effort to find the most proper hostname of this server.
    public static String getHostName() {
        return getHostNameImpl().trim().toLowerCase();
    }

    public static String getIpAddress() {
        try {
            String hostname = getHostName();
            return InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }

    private static String getHostNameImpl() {
        // Return bind address if available
        String bindAddr = System.getProperty("jboss.bind.address");
        if (bindAddr != null && !bindAddr.trim().equals("0.0.0.0")) {
            return bindAddr;
        }

        // Fallback to qualified name
        String qualifiedHostName = System.getProperty("jboss.qualified.host.name");
        if (qualifiedHostName != null) {
            return qualifiedHostName;
        }

        // If not on jboss env, let's try other possible fallbacks
        // POSIX-like OSes including Mac should have this set
        qualifiedHostName = System.getenv("HOSTNAME");
        if (qualifiedHostName != null) {
            return qualifiedHostName;
        }

        // Certain versions of Windows
        qualifiedHostName = System.getenv("COMPUTERNAME");
        if (qualifiedHostName != null) {
            return qualifiedHostName;
        }

        try {
            return NetworkUtils.canonize(getLocalHost().getHostName());
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            return "unknown-host.unknown-domain";
        }
    }

    /**
     * Methods returns InetAddress for localhost
     *
     * @return InetAddress of the localhost
     * @throws UnknownHostException if localhost could not be resolved
     */
    private static InetAddress getLocalHost() throws UnknownHostException {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (ArrayIndexOutOfBoundsException e) {  //this is workaround for mac osx bug see AS7-3223 and JGRP-1404
            addr = InetAddress.getByName(null);
        }
        return addr;
    }
}
