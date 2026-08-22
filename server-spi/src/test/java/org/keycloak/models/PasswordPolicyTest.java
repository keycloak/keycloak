package org.keycloak.models;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

import org.junit.Assert;
import org.junit.Test;

public class PasswordPolicyTest {

    // Only getProvider(Class, String) is exercised by PasswordPolicy.Builder#build; everything
    // else on the (very large) KeycloakSession surface is unused by that code path.
    private static final KeycloakSession SESSION = (KeycloakSession) Proxy.newProxyInstance(
            PasswordPolicyTest.class.getClassLoader(),
            new Class<?>[]{KeycloakSession.class},
            (proxy, method, args) -> {
                if ("getProvider".equals(method.getName())
                        && args != null && args.length == 2 && args[0] == PasswordPolicyProvider.class) {
                    return new NoopPasswordPolicyProvider();
                }
                throw new UnsupportedOperationException(method.getName());
            });

    @Test
    public void policiesArePreservedInConfiguredOrder() {
        List<String> configured = List.of("length", "upperCase", "digits", "passwordHistory", "notUsername");
        String policyString = String.join(" and ", configured);

        PasswordPolicy policy = PasswordPolicy.parse(SESSION, policyString);

        Assert.assertEquals(configured, new ArrayList<>(policy.getPolicies()));
    }

    // PasswordPolicyProvider stub; only parseConfig is invoked by PasswordPolicy.Builder#build.
    private static final class NoopPasswordPolicyProvider implements PasswordPolicyProvider {
        @Override public PolicyError validate(RealmModel realm, UserModel user, String password) { throw new UnsupportedOperationException(); }
        @Override public PolicyError validate(String user, String password) { throw new UnsupportedOperationException(); }
        @Override public Object parseConfig(String value) { return value; }
        @Override public void close() { }
    }
}
