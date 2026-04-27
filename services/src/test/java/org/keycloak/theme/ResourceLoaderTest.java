package org.keycloak.theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class ResourceLoaderTest {

    static String NONE = "../";
    static String SINGLE = "%2E%2E%2F";
    static String DOUBLE = "%252E%252E%252F";

    @Test
    public void testResource() throws IOException {
        String parent = "dummy-resources/parent";
        assertResourceAsStream(parent, "myresource.css", true, true);
        assertResourceAsStream(parent, NONE + "forbidden.css", false, true);
        assertResourceAsStream(parent, SINGLE + "forbidden.css", false, false);
        assertResourceAsStream(parent, DOUBLE + "forbidden.css", false, false);

        assertResourceAsStream(parent, "one/" + NONE + "myresource.css", true, true);
        assertResourceAsStream(parent, "one/" + SINGLE + SINGLE + "forbidden.css", false, false);
        assertResourceAsStream(parent, "one/" + DOUBLE + DOUBLE + "forbidden.css", false, false);

        assertResourceAsStream(parent, "one/two/" + NONE + NONE + "myresource.css", true, true);
        assertResourceAsStream(parent, "one/" + NONE + NONE + "forbidden.css", false, true);
    }

    @Test
    public void testFiles() throws IOException {
        Path tempDirectory = Files.createTempDirectory("safepath-test");

        File parent = new File(tempDirectory.toFile(), "resources");
        Assert.assertTrue(parent.mkdir());

        new FileOutputStream(new File(tempDirectory.toFile(), "forbidden.css")).close();
        new FileOutputStream(new File(parent, "myresource.css")).close();

        assertFileAsStream(parent, "myresource.css", true, true);
        assertFileAsStream(parent, NONE + "forbidden.css", false, true);
        assertFileAsStream(parent, SINGLE + SINGLE + "forbidden.css", false, false);
        assertFileAsStream(parent, DOUBLE + DOUBLE + "forbidden.css", false, false);

        assertFileAsStream(new File(tempDirectory.toFile(), "test/../resources/"), "myresource.css", true, true);

        // relativize tmp folder to the current working directory, something like ../../../tmp/path
        Path currentDir = Paths.get(".").toAbsolutePath();
        if (currentDir.getRoot().equals(parent.toPath().getRoot())) {
            Path relativeParent = currentDir.relativize(parent.toPath());
            assertFileAsStream(relativeParent.toFile(), "myresource.css", true, true);
        } else {
            System.out.println("Skipping relative path test due to temp directory and current directory being on different drives");
        }
    }

    private void assertResourceAsStream(String parent, String resource, boolean expectValid, boolean expectResourceToExist) throws IOException {
        InputStream verified = ResourceLoader.getResourceAsStream(parent, resource);
        if (expectValid) {
            Assert.assertNotNull(verified);
        } else {
            Assert.assertNull(verified);
        }

        if (expectResourceToExist) {
            Assert.assertNotNull(ResourceLoader.class.getClassLoader().getResource(parent + "/" + resource));
        }
    }

    private void assertFileAsStream(File parent, String resource, boolean expectValid, boolean expectFileToExist) throws IOException {
        InputStream verified = ResourceLoader.getFileAsStream(parent, resource);
        if (expectValid) {
            Assert.assertNotNull(verified);
        } else {
            Assert.assertNull(verified);
        }

        if (expectFileToExist) {
            Assert.assertTrue(new File(parent, resource).getCanonicalFile().isFile());
        }
    }

}
