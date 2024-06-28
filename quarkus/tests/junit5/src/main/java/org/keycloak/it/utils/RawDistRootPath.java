package org.keycloak.it.utils;

import java.nio.file.Path;

public class RawDistRootPath {

    private final Path distRootPath;

    public RawDistRootPath(Path path) {
        this.distRootPath = path;
    }
    public Path getDistRootPath() {
        return distRootPath;
    }
}
