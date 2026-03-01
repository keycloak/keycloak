package org.keycloak.policy;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.policy.BlacklistPasswordPolicyProviderFactory.FileBasedPasswordBlacklist;

public class BlacklistPasswordPolicyProviderTest {

    @Test
    public void testUpperCaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt");
        blacklist.lazyInit();

        // all passwords in the deny list are in lower case
        Assert.assertFalse(blacklist.contains("1Password!"));
    }

    @Test
    public void testAlwaysLowercaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt");
        blacklist.lazyInit();
        Assert.assertTrue(blacklist.contains("1Password!".toLowerCase()));
    }

    @Test
    public void testLowerCaseInFile() {
        FileBasedPasswordBlacklist blacklist =
                new FileBasedPasswordBlacklist(Paths.get("src/test/java/org/keycloak/policy"), "short_blacklist.txt");
        blacklist.lazyInit();
        Assert.assertTrue(blacklist.contains("pass1!word"));
    }
}
