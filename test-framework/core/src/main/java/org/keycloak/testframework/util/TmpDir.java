package org.keycloak.testframework.util;

import java.io.File;

public class TmpDir {

    // Maven overrides java.io.tmpdir, resolving to OS tmp directory
    public static File resolveTmpDir() {
        File tmpDir = new File("/tmp");
        if (tmpDir.isDirectory()) {
            return tmpDir;
        }
        tmpDir = new File(System.getenv("TEMP"));
        if (tmpDir.isDirectory()) {
            return tmpDir;
        }
        return new File(System.getProperty("java.io.tmpdir"));
    }

}
