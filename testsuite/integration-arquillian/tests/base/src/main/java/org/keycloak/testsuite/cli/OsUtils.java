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
package org.keycloak.testsuite.cli;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class OsUtils {

    public static OsArch determineOSAndArch() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");

        //System.out.println("OS: " + os + ", architecture: " + arch);
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
        } else if (os.startsWith("openbsd")) {
            return new OsArch("openbsd", arch);
        }

        // unsupported platform
        throw new RuntimeException("Could not determine OS and architecture for this operating system: " + os);
    }
}
