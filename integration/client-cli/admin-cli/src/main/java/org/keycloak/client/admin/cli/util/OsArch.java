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
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class OsArch {

    private String os;
    private String arch;
    private boolean legacy;

    public OsArch(String os, String arch) {
        this(os, arch, false);
    }

    public OsArch(String os, String arch, boolean legacy) {
        this.os = os;
        this.arch = arch;
        this.legacy = legacy;
    }

    public String os() {
        return os;
    }

    public String arch() {
        return arch;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public boolean isWindows() {
        return "win32".equals(os);
    }

    public String envVar(String var) {
        if (isWindows()) {
            return "%" + var + "%";
        } else {
            return "$" + var;
        }
    }

    public String path(String path) {
        if (isWindows()) {
            path = path.replaceAll("/", "\\\\");
            if (path.startsWith("~")) {
                path =  "%HOMEPATH%" + path.substring(1);
            }
        }
        return path;
    }
}