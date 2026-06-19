package org.keycloak.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import org.keycloak.policy.DenylistPasswordPolicyProviderFactory.FileBasedPasswordDenylist;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DenylistPasswordPolicyProviderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testPasswordLookupIsCaseInsensitive() {
        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(Paths.get("src/test/java/org/keycloak/policy"), "short_denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        DenylistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);

        // passwords in the deny list are stored in lower case; lookups must be case-insensitive
        Assert.assertFalse(denylist.contains("1Password!"));
        Assert.assertTrue(denylist.contains("1Password!".toLowerCase()));
    }

    @Test
    public void testReloadOnFileMtimeChange() throws Exception {
        Path file = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(file, "oldpassword\n");

        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1); // Use 1 msec check interval for not to delay the test too much.

        Assert.assertTrue(denylist.contains("oldpassword"));
        Assert.assertFalse(denylist.contains("newpassword"));

        // Wait for the check interval to elapse.
        // Rewrite the file, and bump the mtime by 1 sec to ensure a detectable change regardless of the filesystem's mtime granularity.
        Thread.sleep(2);
        Files.writeString(file, "newpassword\n");
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 1000));

        Assert.assertFalse(denylist.contains("oldpassword"));
        Assert.assertTrue(denylist.contains("newpassword"));
    }

    @Test
    public void testReloadOnFileSizeChange() throws Exception {
        Path file = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(file, "oldpassword\n");
        FileTime originalMtime = Files.getLastModifiedTime(file);

        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1); // Use 1 msec check interval for not to delay the test too much.

        Assert.assertTrue(denylist.contains("oldpassword"));
        Assert.assertFalse(denylist.contains("newpassword"));

        // Wait for the check interval to elapse.
        // Rewrite the content while preserving the original mtime to simulate filesystems
        // with coarse granularity where the timestamp remains unchanged between writes.
        Thread.sleep(2);
        Files.writeString(file, "newpassword\nanotherpassword\n");
        Files.setLastModifiedTime(file, originalMtime);

        Assert.assertFalse(denylist.contains("oldpassword"));
        Assert.assertTrue(denylist.contains("newpassword"));
    }

    @Test
    public void testReloadCheckIntervalZero() throws Exception {
        Path file = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(file, "oldpassword\n");

        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);

        Assert.assertTrue(denylist.contains("oldpassword"));

        Files.writeString(file, "newpassword\n");
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 1000));

        Thread.sleep(2);
        Assert.assertTrue(denylist.contains("oldpassword"));
        Assert.assertFalse(denylist.contains("newpassword"));
    }

    @Test
    public void testLoadFromBloomFile() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "secret123\nbadpassword\n");
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);

        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt.bloom",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);

        Assert.assertTrue("Should find password from .bloom file", denylist.contains("secret123"));
        Assert.assertTrue("Should find password from .bloom file", denylist.contains("badpassword"));
        Assert.assertFalse("Should not find password that was never added", denylist.contains("goodpassword"));
    }

    @Test(expected = RuntimeException.class)
    public void testCorruptBloomFileThrows() throws Exception {
        Path bloomFile = tempFolder.newFile("denylist.txt.bloom").toPath();
        Files.writeString(bloomFile, "this is not a valid bloom filter binary");

        new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt.bloom",
                DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);
    }

    @Test
    public void testReloadBloomFileOnChange() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "initialpassword\n");
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);

        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(tempFolder.getRoot().toPath(), "denylist.txt.bloom",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1);

        Assert.assertTrue(denylist.contains("initialpassword"));
        Assert.assertFalse(denylist.contains("updatedpassword"));

        Thread.sleep(2);
        Files.writeString(txtFile, "updatedpassword\n");
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);
        Path bloomFile = txtFile.resolveSibling("denylist.txt.bloom");
        Files.setLastModifiedTime(bloomFile, FileTime.fromMillis(Files.getLastModifiedTime(bloomFile).toMillis() + 1000));

        Assert.assertFalse("Old password should no longer be found after reload", denylist.contains("initialpassword"));
        Assert.assertTrue("Updated password should be found after reload", denylist.contains("updatedpassword"));
    }

    @Test
    public void testBuildBloomFileCreatesReadableOutput() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "alpha\nbeta\ngamma\n");

        Path bloomFile = txtFile.resolveSibling("denylist.txt.bloom");
        DenylistPasswordPolicyProviderFactory.buildBloomFile(
                txtFile, bloomFile, DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);
        Assert.assertTrue("bloom file must be created", Files.exists(bloomFile));
        Assert.assertTrue("bloom file must have non-zero size", Files.size(bloomFile) > 0);
    }

    @Test(expected = IOException.class)
    public void testBuildBloomFileMissingInputThrows() throws Exception {
        Path missing = tempFolder.getRoot().toPath().resolve("nonexistent.txt");
        DenylistPasswordPolicyProviderFactory.buildBloomFile(
                missing, missing.resolveSibling("nonexistent.txt.bloom"),
                DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);
    }

    @Test
    public void testFppMismatchStillLoadsSuccessfully() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "mismatchpw\n");

        double bloomFpp = 0.01;
        double serverFpp = DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY;
        DenylistPasswordPolicyProviderFactory.buildBloomFile(txtFile, txtFile.resolveSibling("denylist.txt.bloom"), bloomFpp);

        FileBasedPasswordDenylist denylist = new FileBasedPasswordDenylist(
                tempFolder.getRoot().toPath(), "denylist.txt.bloom", serverFpp, 0);
        Assert.assertTrue("password must still be found despite fpp mismatch", denylist.contains("mismatchpw"));
    }

    private static void writeBloomFile(Path baseDir, String name, double fpp) throws IOException {
        Path input = baseDir.resolve(name);
        DenylistPasswordPolicyProviderFactory.buildBloomFile(input, input.resolveSibling(name + ".bloom"), fpp);
    }

}
