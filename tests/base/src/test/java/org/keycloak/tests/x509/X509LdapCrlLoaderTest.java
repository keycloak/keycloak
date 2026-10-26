package org.keycloak.tests.x509;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.x509.CertificateValidator;
import org.keycloak.authentication.authenticators.x509.ValidateX509CertificateUsernameFactory;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForServers;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

/**
 * CRLs loaded from LDAP must be limited in size and time. The CRL loader used directly by the tests has the default
 * limits, while the size limit of the direct grant authenticator is lowered through its provider option.
 */
@KeycloakIntegrationTest(config = X509LdapCrlLoaderTest.LdapCrlServerConfig.class)
@DisabledForServers("remote") // the LDAP server runs inside the test JVM and is not reachable from a remote server
public class X509LdapCrlLoaderTest extends AbstractX509AuthenticationTest {

    @InjectRunOnServer(ref = "crl-loader", permittedPackages = "org.keycloak.tests.x509")
    RunOnServerClient crlLoader;

    @Test
    public void ldapCrlWithinSizeLimitIsLoaded() throws Exception {
        try (LdapCrlServer ldap = LdapCrlServer.servingCrl(CrlGenerator.generateValidCrl())) {
            String result = crlLoader.fetch(CrlFileLoaderProbe.loadCrl(ldap.crlUrl()), String.class);
            Assertions.assertEquals("LOADED:1", result, "A CRL served by LDAP must be loaded");
        }
    }

    @Test
    public void ldapCrlExceedingSizeLimitIsRejected() throws Exception {
        byte[] crl = CrlGenerator.generateValidCrl((int) CertificateValidator.LdapContext.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE + 1024);
        try (LdapCrlServer ldap = LdapCrlServer.servingCrl(crl)) {
            String result = crlLoader.fetch(CrlFileLoaderProbe.loadCrl(ldap.crlUrl()), String.class);
            Assertions.assertFalse(result.startsWith("LOADED"),
                    "A CRL exceeding the size limit was read from LDAP -> " + result);
            Assertions.assertTrue(result.contains("Unable to load CRL"),
                    "Expected the oversized CRL to be rejected, but got " + result);
        }
    }

    @Test
    public void unresponsiveLdapServerTimesOut() throws Exception {
        try (LdapCrlServer ldap = LdapCrlServer.unresponsive()) {
            String result = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(60),
                    () -> crlLoader.fetch(CrlFileLoaderProbe.loadCrl(ldap.crlUrl()), String.class),
                    "Loading a CRL from an unresponsive LDAP server must time out");
            Assertions.assertTrue(result.contains("Unable to load CRL"),
                    "Expected the CRL loading to fail after the timeout, but got " + result);
        }
    }

    @Test
    public void authenticatorLdapCrlSizeLimitIsConfigurable() throws Exception {
        // the CRL is within the default limit, but exceeds the limit configured for the direct grant authenticator
        byte[] crl = Files.readAllBytes(Paths.get(getClass().getResource("/" + INTERMEDIATE_CA_CRL_PATH).toURI()));
        try (LdapCrlServer ldap = LdapCrlServer.servingCrl(crl)) {
            X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel()
                    .setCRLEnabled(true)
                    .setCRLRelativePath(ldap.crlUrl())
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
            String configId = createConfig(directGrantExecution.getId(), newConfig("x509-ldap-crl", config.getConfig()));
            managedRealm.cleanup().add(r -> r.flows().removeAuthenticatorConfig(configId));

            AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

            Assertions.assertEquals(401, response.getStatusCode());
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            Assertions.assertEquals("Unable to load CRL", response.getErrorDescription());
        }
    }

    public static class LdapCrlServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("authenticator", ValidateX509CertificateUsernameFactory.PROVIDER_ID, "crl-ldap-max-consumed-response-size", "100");
        }
    }
}
