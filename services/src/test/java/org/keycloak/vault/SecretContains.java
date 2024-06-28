package org.keycloak.vault;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.nio.charset.StandardCharsets;

/**
 * Checks if {@link VaultRawSecret} is equal to a String.
 */
public class SecretContains extends TypeSafeMatcher<VaultRawSecret> {

    private String thisVaultAsString;

    public SecretContains(String thisVaultAsString) {
        this.thisVaultAsString = thisVaultAsString;
    }

    @Override
    protected boolean matchesSafely(VaultRawSecret secret) {
        String convertedSecret = StandardCharsets.UTF_8.decode(secret.get().get()).toString();
        return thisVaultAsString.equals(convertedSecret);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is equal to " + thisVaultAsString);
    }

    public static Matcher<VaultRawSecret> secretContains(String thisVaultAsString) {
        return new SecretContains(thisVaultAsString);
    }
}
