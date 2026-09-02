package org.keycloak.db.compatibility.verifier;

import java.io.File;

public class UnsupportedMojoTest extends AbstractNewEntryMojoTest {

    UnsupportedMojoTest() {
        super(new UnsupportedMojo());
    }

    @Override
    protected File getTargetFile() {
        return unsupportedFile;
    }

    @Override
    protected File getAlternateFile() {
        return supportedFile;
    }
}
