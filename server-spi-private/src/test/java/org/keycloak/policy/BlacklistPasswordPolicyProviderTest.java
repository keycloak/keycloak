package org.keycloak.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory.FileBasedPasswordBlacklist;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BlacklistPasswordPolicyProviderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testUpperCaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);

        // all passwords in the deny list are in lower case
        Assert.assertFalse(blacklist.contains("1Password!"));
    }

    @Test
    public void testAlwaysLowercaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);
        Assert.assertTrue(blacklist.contains("1Password!".toLowerCase()));
    }

    @Test
    public void testLowerCaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);
        Assert.assertTrue(blacklist.contains("pass1!word"));
    }

    @Test
    public void testReloadOnFileMtimeChange() throws Exception {
        Path file = tempFolder.newFile("blacklist.txt").toPath();
        Files.writeString(file, "oldpassword\n");

        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1); // Use 1 msec check interval for not to delay the test too much.

        Assert.assertTrue(blacklist.contains("oldpassword"));
        Assert.assertFalse(blacklist.contains("newpassword"));

        // Wait for the check interval to elapse.
        // Rewrite the file, and bump the mtime by 1 sec to ensure a detectable change regardless of the filesystem's mtime granularity.
        Thread.sleep(2);
        Files.writeString(file, "newpassword\n");
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 1000));

        Assert.assertFalse(blacklist.contains("oldpassword"));
        Assert.assertTrue(blacklist.contains("newpassword"));
    }

    @Test
    public void testReloadOnFileSizeChange() throws Exception {
        Path file = tempFolder.newFile("blacklist.txt").toPath();
        Files.writeString(file, "oldpassword\n");
        FileTime originalMtime = Files.getLastModifiedTime(file);

        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1); // Use 1 msec check interval for not to delay the test too much.

        Assert.assertTrue(blacklist.contains("oldpassword"));
        Assert.assertFalse(blacklist.contains("newpassword"));

        // Wait for the check interval to elapse.
        // Rewrite the content while preserving the original mtime to simulate filesystems
        // with coarse granularity where the timestamp remains unchanged between writes.
        Thread.sleep(2);
        Files.writeString(file, "newpassword\nanotherpassword\n");
        Files.setLastModifiedTime(file, originalMtime);

        Assert.assertFalse(blacklist.contains("oldpassword"));
        Assert.assertTrue(blacklist.contains("newpassword"));
    }

    @Test
    public void testReloadCheckIntervalZero() throws Exception {
        Path file = tempFolder.newFile("blacklist.txt").toPath();
        Files.writeString(file, "oldpassword\n");

        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "blacklist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);

        Assert.assertTrue(blacklist.contains("oldpassword"));

        Files.writeString(file, "newpassword\n");
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 1000));

        Thread.sleep(2);
        Assert.assertTrue(blacklist.contains("oldpassword"));
        Assert.assertFalse(blacklist.contains("newpassword"));
    }

    @Test
    public void testLoadFromBloomFile() throws Exception {
        // Write plaintext denylist
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "secret123\nbadpassword\n");

        // Pre-compute and write .bloom file
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);

        FileBasedPasswordBlacklist denylist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "denylist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);

        Assert.assertTrue("Should find password from .bloom file", denylist.contains("secret123"));
        Assert.assertTrue("Should find password from .bloom file", denylist.contains("badpassword"));
        Assert.assertFalse("Should not find password that was never added", denylist.contains("goodpassword"));
    }

    @Test
    public void testCorruptBloomFileFallsBackToPlaintext() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "plaintextpassword\n");

        // Write a corrupt (non-Guava) .bloom file
        Path bloomFile = txtFile.resolveSibling("denylist.txt.bloom");
        Files.writeString(bloomFile, "this is not a valid bloom filter binary");

        FileBasedPasswordBlacklist denylist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "denylist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);

        // Should have fallen back to plaintext load — password must still be found
        Assert.assertTrue("Fallback to plaintext should still find the password",
                denylist.contains("plaintextpassword"));
    }

    @Test
    public void testReloadWatchesBloomFileWhenPresent() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "initialpassword\n");
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);

        FileBasedPasswordBlacklist denylist =
                new FileBasedPasswordBlacklist(tempFolder.getRoot().toPath(), "denylist.txt",
                        BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 1);

        Assert.assertTrue(denylist.contains("initialpassword"));
        Assert.assertFalse(denylist.contains("updatedpassword"));

        // Update the plaintext file and regenerate the .bloom file
        Thread.sleep(2);
        Files.writeString(txtFile, "updatedpassword\n");
        writeBloomFile(tempFolder.getRoot().toPath(), "denylist.txt",
                BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);
        // Bump mtime on .bloom to ensure change detection
        Path bloomFile = txtFile.resolveSibling("denylist.txt.bloom");
        Files.setLastModifiedTime(bloomFile, FileTime.fromMillis(Files.getLastModifiedTime(bloomFile).toMillis() + 1000));

        Assert.assertFalse("Old password should no longer be found after reload", denylist.contains("initialpassword"));
        Assert.assertTrue("Updated password should be found after reload", denylist.contains("updatedpassword"));
    }

    @Test
    public void testBuildBloomFileCreatesReadableOutput() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "alpha\nbeta\ngamma\n");

        BlacklistPasswordPolicyProviderFactory.buildBloomFile(
                txtFile, BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);

        Path bloomFile = txtFile.resolveSibling("denylist.txt.bloom");
        Assert.assertTrue("bloom file must be created", Files.exists(bloomFile));
        Assert.assertTrue("bloom file must have non-zero size", Files.size(bloomFile) > 0);

        // The server must be able to load and use it
        FileBasedPasswordBlacklist bl = new FileBasedPasswordBlacklist(
                tempFolder.getRoot().toPath(), "denylist.txt",
                BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY, 0);
        Assert.assertTrue(bl.contains("alpha"));
        Assert.assertTrue(bl.contains("beta"));
        Assert.assertFalse(bl.contains("delta"));
    }

    @Test(expected = IOException.class)
    public void testBuildBloomFileMissingInputThrows() throws Exception {
        Path missing = tempFolder.getRoot().toPath().resolve("nonexistent.txt");
        BlacklistPasswordPolicyProviderFactory.buildBloomFile(
                missing, BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY);
    }

    @Test
    public void testFppMismatchStillLoadsSuccessfully() throws Exception {
        Path txtFile = tempFolder.newFile("denylist.txt").toPath();
        Files.writeString(txtFile, "mismatchpw\n");

        // Build bloom with a DIFFERENT fpp than what the server will use
        double bloomFpp = 0.01;    // coarser
        double serverFpp = BlacklistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY; // 0.0001
        BlacklistPasswordPolicyProviderFactory.buildBloomFile(txtFile, bloomFpp);

        // Server should still load without throwing (only logs a warning)
        FileBasedPasswordBlacklist bl = new FileBasedPasswordBlacklist(
                tempFolder.getRoot().toPath(), "denylist.txt", serverFpp, 0);
        Assert.assertTrue("password must still be found despite fpp mismatch", bl.contains("mismatchpw"));
    }

    private static void writeBloomFile(Path baseDir, String name, double fpp) throws IOException {
        BlacklistPasswordPolicyProviderFactory.buildBloomFile(baseDir.resolve(name), fpp);
    }

}
