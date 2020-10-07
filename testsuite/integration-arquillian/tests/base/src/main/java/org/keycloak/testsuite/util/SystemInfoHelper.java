/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;


/**
 * Provides some data about CPU and Memory of the java process used for the testsuite
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemInfoHelper {

    public static String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();

        StringBuilder s = new StringBuilder("TEST PROCESS INFO: ");
        s.append("\nAvailable processors: " + runtime.availableProcessors());
        s.append("\nTotal memory: " + toMB(runtime.totalMemory()));
        s.append("\nMax memory (Xmx): " + toMB(runtime.maxMemory()));

        for (MemoryPoolMXBean memoryMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(memoryMXBean.getName())) {
                s.append("\nMetaspace Max: " + toMB(memoryMXBean.getUsage().getMax()));
            }
        }

        return s.toString();
    }


    private static String toMB(long bytes) {
        return bytes / 1024 / 1024 + " MB";
    }

}
