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

package org.keycloak.representations.info;

public class MemoryInfoRepresentation {

    protected long total;
    protected String totalFormated;
    protected long used;
    protected String usedFormated;
    protected long free;
    protected long freePercentage;
    protected String freeFormated;

    public static MemoryInfoRepresentation create() {
        MemoryInfoRepresentation rep = new MemoryInfoRepresentation();
        Runtime runtime = Runtime.getRuntime();
        rep.total = runtime.maxMemory();
        rep.totalFormated = formatMemory(rep.total);
        rep.used = runtime.totalMemory() - runtime.freeMemory();
        rep.usedFormated = formatMemory(rep.used);
        rep.free = rep.total - rep.used;
        rep.freeFormated = formatMemory(rep.free);
        rep.freePercentage = rep.free * 100 / rep.total;
        return rep;
    }

    public long getTotal() {
        return total;
    }

    public String getTotalFormated() {
        return totalFormated;
    }

    public long getFree() {
        return free;
    }

    public String getFreeFormated() {
        return freeFormated;
    }

    public long getUsed() {
        return used;
    }

    public String getUsedFormated() {
        return usedFormated;
    }

    public long getFreePercentage() {
        return freePercentage;
    }

    private static String formatMemory(long bytes) {
        if (bytes > 1024L * 1024L) {
            return bytes / (1024L * 1024L) + " MB";
        } else if (bytes > 1024L) {
            return bytes / (1024L) + " kB";
        } else {
            return bytes + " B";
        }
    }

}
