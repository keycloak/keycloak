package org.keycloak.policy;

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
    public void testUpperCaseInFile() {
        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(Paths.get("src/test/java/org/keycloak/policy"), "short_denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        DenylistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);

        // all passwords in the deny list are in lower case
        Assert.assertFalse(denylist.contains("1Password!"));
    }

    @Test
    public void testAlwaysLowercaseInFile() {
        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(Paths.get("src/test/java/org/keycloak/policy"), "short_denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        DenylistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);
        Assert.assertTrue(denylist.contains("1Password!".toLowerCase()));
    }

    @Test
    public void testLowerCaseInFile() {
        FileBasedPasswordDenylist denylist =
                new FileBasedPasswordDenylist(Paths.get("src/test/java/org/keycloak/policy"), "short_denylist.txt",
                        DenylistPasswordPolicyProviderFactory.DEFAULT_FALSE_POSITIVE_PROBABILITY,
                        DenylistPasswordPolicyProviderFactory.DEFAULT_CHECK_INTERVAL_SECONDS * 1000L);
        Assert.assertTrue(denylist.contains("pass1!word"));
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

}
