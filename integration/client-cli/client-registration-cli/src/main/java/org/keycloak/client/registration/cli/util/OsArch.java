/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.keycloak.client.registration.cli.util;

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