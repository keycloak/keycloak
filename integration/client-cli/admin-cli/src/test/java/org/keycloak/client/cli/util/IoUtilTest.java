package org.keycloak.client.cli.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IoUtilTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void assumePosix() {
        Assume.assumeTrue("POSIX filesystem required",
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    }

    @Test
    public void ensureFileCreatesNewFileWithOwnerOnlyPermissions() throws IOException {
        Path configFile = tempFolder.getRoot().toPath().resolve("newdir/config.json");

        IoUtil.ensureFile(configFile);

        assertThat(Files.isRegularFile(configFile), is(true));
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(configFile)), is("rw-------"));
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(configFile.getParent())), is("rwx------"));
    }

    @Test
    public void ensureFileCorrectsPermissionsOnExistingFile() throws IOException {
        Path dir = tempFolder.newFolder("existing").toPath();
        Path configFile = dir.resolve("config.json");
        Files.createFile(configFile);

        Files.setPosixFilePermissions(configFile, PosixFilePermissions.fromString("rw-r--r--"));

        IoUtil.ensureFile(configFile);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(configFile)), is("rw-------"));
    }

    @Test
    public void ensureFilePreservesOwnerOnlyPermissions() throws IOException {
        Path dir = tempFolder.newFolder("secure").toPath();
        Path configFile = dir.resolve("config.json");
        Files.createFile(configFile);

        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(configFile, PosixFilePermissions.fromString("rw-------"));

        IoUtil.ensureFile(configFile);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(configFile)), is("rw-------"));
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)), is("rwx------"));
    }
}
