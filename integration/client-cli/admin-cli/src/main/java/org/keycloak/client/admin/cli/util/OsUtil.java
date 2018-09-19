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
package org.keycloak.client.admin.cli.util;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class OsUtil {

    public static final OsArch OS_ARCH = determineOSAndArch();
    // TODO: move CMD out of this class
    public static final String CMD = OS_ARCH.isWindows() ? "kcadm.bat" : "kcadm.sh";

    public static final String PROMPT = OS_ARCH.isWindows() ? "c:\\>" : "$";

    public static final String EOL = OS_ARCH.isWindows() ? "\r\n" : "\n";


    public static OsArch determineOSAndArch() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");

        if (arch.equals("amd64")) {
            arch = "x86_64";
        }

        if (os.startsWith("linux")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586")) {
                arch = "i686";
            }
            return new OsArch("linux", arch);
        } else if (os.startsWith("windows")) {
            if (arch.equals("x86")) {
                arch = "i386";
            }
            if (os.indexOf("2008") != -1 || os.indexOf("2003") != -1 || os.indexOf("vista") != -1) {
                return new OsArch("win32", arch, true);
            } else {
                return new OsArch("win32", arch);
            }
        } else if (os.startsWith("sunos")) {
            return new OsArch("sunos5", "x86_64");
        } else if (os.startsWith("mac os x")) {
            return new OsArch("osx", "x86_64");
        } else if (os.startsWith("freebsd")) {
            return new OsArch("freebsd", arch);
        }

        // unsupported platform
        throw new RuntimeException("Could not determine OS and architecture for this operating system: " + os);
    }
}
