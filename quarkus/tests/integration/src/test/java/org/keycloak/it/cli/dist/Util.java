package org.keycloak.it.cli.dist;

import java.io.File;
import java.io.IOException;

public class Util {
    static File createTempFile(String prefix, String suffix) throws IOException {
        var file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        return file;
    }
}
