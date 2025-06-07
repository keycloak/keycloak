package org.keycloak.test.logparser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Utils {

    public static List<String> readLines(File file) throws IOException {
        return Files.lines(file.toPath()).toList();
    }

}
