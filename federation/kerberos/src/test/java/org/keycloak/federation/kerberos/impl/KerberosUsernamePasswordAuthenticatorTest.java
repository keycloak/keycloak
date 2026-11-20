package org.keycloak.federation.kerberos.impl;

import java.util.Locale;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.federation.kerberos.KerberosConfig;

import org.junit.Assert;
import org.junit.Test;

public class KerberosUsernamePasswordAuthenticatorTest {

    @Test
    public void usernameWithoutRealmGetsConfiguredRealm() throws LoginException {
        Assert.assertEquals("jduke@KEYCLOAK.ORG", authenticator().getKerberosPrincipal("jduke"));
    }

    @Test
    public void backslashElsewhereInTheNameIsKept() throws LoginException {
        Assert.assertEquals("KEYCLOAK\\jduke@KEYCLOAK.ORG", authenticator().getKerberosPrincipal("KEYCLOAK\\jduke"));
    }

    @Test
    public void configuredRealmIsAcceptedIgnoringCase() throws LoginException {
        Assert.assertEquals("jduke@KEYCLOAK.ORG", authenticator().getKerberosPrincipal("jduke@keycloak.org"));
    }

    @Test
    public void allowedRealmIsAccepted() throws LoginException {
        Assert.assertEquals("jduke@KC2.COM", authenticator("KC2.COM").getKerberosPrincipal("jduke@kc2.com"));
    }

    @Test
    public void allowedRealmsAreTrimmedAndBlankEntriesIgnored() throws LoginException {
        Assert.assertEquals("jduke@KC2.COM", authenticator(" kc2.com ", "").getKerberosPrincipal("jduke@KC2.COM"));
    }

    @Test
    public void realmIsCanonicalizedIndependentlyOfTheDefaultLocale() throws LoginException {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            Assert.assertEquals("jduke@INVALID.EXAMPLE", authenticator("invalid.example").getKerberosPrincipal("jduke@invalid.example"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test(expected = LoginException.class)
    public void lookalikeRealmIsRejected() throws LoginException {
        authenticator("KC2.COM").getKerberosPrincipal("jduke@\u212AC2.COM");
    }

    @Test(expected = LoginException.class)
    public void realmNotAllowedIsRejected() throws LoginException {
        authenticator("KC2.COM").getKerberosPrincipal("jduke@KC3.COM");
    }

    @Test
    public void realmNotAllowedIsRejectedBeforeContactingKdc() {
        KerberosUsernamePasswordAuthenticator authenticator = authenticator();
        Assert.assertFalse(authenticator.isUserAvailable("jduke@KC2.COM"));
        Assert.assertFalse(authenticator.validUser("jduke@KC2.COM", "theduke"));
    }

    @Test
    public void malformedPrincipalIsRejectedBeforeContactingKdc() {
        KerberosUsernamePasswordAuthenticator authenticator = authenticator();
        for (String username : new String[] { "jduke@", "a@b@c", "@KEYCLOAK.ORG", "jduke\\@KEYCLOAK.ORG", "jduke\\" }) {
            Assert.assertFalse(username, authenticator.isUserAvailable(username));
            Assert.assertFalse(username, authenticator.validUser(username, "theduke"));
        }
    }

    // The JAAS login is the first step that contacts a KDC, so it must never be reached for a realm that is not allowed
    private static KerberosUsernamePasswordAuthenticator authenticator(String... allowedRealms) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(KerberosConstants.KERBEROS_REALM, "KEYCLOAK.ORG");
        for (String realm : allowedRealms) {
            config.add(KerberosConstants.ALLOWED_KERBEROS_REALMS, realm);
        }
        ComponentModel model = new ComponentModel();
        model.setConfig(config);
        return new KerberosUsernamePasswordAuthenticator(new KerberosConfig(model)) {
            @Override
            protected Configuration createJaasConfiguration() {
                throw new AssertionError("KDC must not be contacted");
            }
        };
    }
}
