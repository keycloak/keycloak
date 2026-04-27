package org.keycloak.vault;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A small helper class to navigate to proper vault directory.
 *
 * @author Sebastian Łaskawiec
 */
enum Scenario {
    EXISTING("src/test/resources/org/keycloak/vault"),
    NON_EXISTING("src/test/resources/org/keycloak/vault/non-existing"),
    WRITABLE_IN_RUNTIME("target/test-classes");

    Path path;

    Scenario(String path) {
        this.path = Paths.get(path);
    }

    public Path getPath() {
        return path;
    }

    public String getAbsolutePathAsString() {
        return path.toAbsolutePath().toString();
    }

}
