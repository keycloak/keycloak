package org.keycloak.db.compatibility.verifier;

import java.io.File;

public class SupportedMojoTest extends AbstractNewEntryMojoTest {

    SupportedMojoTest() {
        super(new SupportedMojo());
    }

    @Override
    protected File getTargetFile() {
        return supportedFile;
    }

    @Override
    protected File getAlternateFile() {
        return unsupportedFile;
    }
}
