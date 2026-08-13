/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.client.cli.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClassLoaderUtil {

    /**
     * Detect if BC FIPS jars are present in the given directory. Return classloader with appropriate JARS based on that
     */
    public static ClassLoader resolveClassLoader(String libDir) {
        File[] jarsInDir = new File(libDir).listFiles(file -> file.getName().endsWith(".jar"));

        // Detect if BC FIPS jars are present in the "client/lib" directory
        boolean bcFipsJarPresent = Stream.of(jarsInDir).anyMatch(file -> file.getName().startsWith("bc-fips"));
        String[] validJarPrefixes = bcFipsJarPresent ?  new String[] {"keycloak-crypto-fips1402", "bc-fips", "bctls-fips","bcutil-fips"} : new String[] {"keycloak-crypto-default", "bcprov-jdk18on"};
        URL[] usedJars = Stream.of(jarsInDir)
                .filter(file -> {
                    for (String prefix : validJarPrefixes) {
                        if (file.getName().startsWith(prefix + "-")) return true;
                    }
                    return false;
                })
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException ex) {
                        throw new IllegalStateException("Error when converting file into URL. Please check the files in the directory " + jarsInDir, ex);
                    }
                }).toArray(URL[]::new);

        return new URLClassLoader(usedJars, ClassLoaderUtil.class.getClassLoader());
    }

}
