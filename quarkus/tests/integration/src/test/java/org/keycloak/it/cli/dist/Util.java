package org.keycloak.it.cli.dist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Util {
    static File createTempFile(String prefix, String suffix) throws IOException {
        var file = Files.createTempFile(prefix, suffix).toFile();
        file.deleteOnExit();
        return file;
    }
}
