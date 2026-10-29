package org.keycloak.tests.admin.partialimport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.partialimport.ResourceType;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = AbstractPartialImportTest.PartialImportServerConfig.class)
public class PartialImportProvidersTest extends AbstractPartialImportTest {

    @Test
    @DatabaseTest
    public void testAddProviders() {
        setFail();
        addProviders();

        PartialImportResults results = doImport();
        assertEquals(IDP_ALIASES.length, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            IdentityProviderResource idpRsc = managedRealm.admin().identityProviders().get(id);
            IdentityProviderRepresentation idp = idpRsc.toRepresentation();
            Map<String, String> config = idp.getConfig();
            assertTrue(Arrays.asList(IDP_ALIASES).contains(config.get("clientId")));
        }
    }

    /**
     * Builds an OIDC provider that deliberately omits validateSignature. With a JWKS URL it is valid; without one
     * (useJwksUrl=false and no public key) it is invalid once the secure default (absent means "validate") applies.
     */
    private static IdentityProviderRepresentation oidcProviderWithoutSignatureSettings(String alias, boolean withJwksUrl) {
        IdentityProviderRepresentation oidc = new IdentityProviderRepresentation();
        oidc.setAlias(alias);
        oidc.setProviderId(OIDCIdentityProviderFactory.PROVIDER_ID);
        oidc.setEnabled(true);
        Map<String, String> oidcConfig = new HashMap<>();
        oidcConfig.put("clientId", "oidc-client");
        oidcConfig.put("clientSecret", "secret");
        oidcConfig.put(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.toString(withJwksUrl));
        if (withJwksUrl) {
            oidcConfig.put(OIDCIdentityProviderConfig.JWKS_URL, "https://localhost/jwks");
        }
        oidc.setConfig(oidcConfig);
        return oidc;
    }

    @Test
    public void testAddProvidersWithoutSignatureSettingsKeepsThemAbsent() {
        setFail();

        IdentityProviderRepresentation oidc = oidcProviderWithoutSignatureSettings("oidc-without-signature-settings", true);

        IdentityProviderRepresentation saml = new IdentityProviderRepresentation();
        saml.setAlias("saml-without-signature-settings");
        saml.setProviderId(SAMLIdentityProviderFactory.PROVIDER_ID);
        saml.setEnabled(true);
        Map<String, String> samlConfig = new HashMap<>();
        samlConfig.put(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "https://localhost/saml");
        saml.setConfig(samlConfig);

        piRep.setIdentityProviders(List.of(oidc, saml));

        PartialImportResults results = doImport();
        assertEquals(2, results.getAdded(), results.getErrorMessage());

        Map<String, String> storedOidc = managedRealm.admin().identityProviders().get(oidc.getAlias()).toRepresentation().getConfig();

        assertFalse(storedOidc.containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));

        Map<String, String> storedSaml = managedRealm.admin().identityProviders().get(saml.getAlias()).toRepresentation().getConfig();

        assertFalse(storedSaml.containsKey(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
        assertFalse(storedSaml.containsKey(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED));
    }

    @Test
    public void testAddOidcProviderWithoutSignatureSettingsAndVerificationKeyIsRejected() {
        setFail();

        IdentityProviderRepresentation oidc = oidcProviderWithoutSignatureSettings("oidc-invalid-without-signature-settings", false);

        piRep.setIdentityProviders(List.of(oidc));

        try (Response response = managedRealm.admin().partialImport(piRep)) {
            assertFalse(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL, "Partial import of an invalid OIDC provider must not succeed");
        }
        assertTrue(managedRealm.admin().identityProviders().findAll().stream().noneMatch(idp -> oidc.getAlias().equals(idp.getAlias())), "The rejected identity provider must not be persisted");

        oidc.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        PartialImportResults results = doImport();
        assertEquals(1, results.getAdded(), results.getErrorMessage());
    }

    @Test
    @DatabaseTest
    public void testAddProviderMappers() {
        setFail();
        addProviderMappers();

        PartialImportResults results = doImport();
        assertEquals(IDP_ALIASES.length*2, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            if (ResourceType.IDP.equals(result.getResourceType())) {
                String id = result.getId();
                IdentityProviderResource idpRsc = managedRealm.admin().identityProviders().get(id);
                IdentityProviderMapperRepresentation idpMap = idpRsc.getMappers().get(0);
                String alias = idpMap.getIdentityProviderAlias();
                assertTrue(Arrays.asList(IDP_ALIASES).contains(alias));
                assertEquals(alias + "_mapper", idpMap.getName());
                assertEquals("keycloak-oidc-role-to-role-idp-mapper", idpMap.getIdentityProviderMapper());
                assertEquals("IDP.TEST_ROLE", idpMap.getConfig().get("external.role"));
                assertEquals("FORCE", idpMap.getConfig().get("syncMode"));
                assertEquals("TEST_ROLE", idpMap.getConfig().get("role"));
            }
        }
    }

    @Test
    public void testAddProvidersFail() {
        addProviders();
        testFail();
    }

    @Test
    public void testAddProviderMappersFail() {
        addProviderMappers();
        testFail();
    }

    @Test
    public void testAddProvidersSkip() {
        addProviders();
        testSkip();
    }

    @Test
    public void testAddProviderMappersSkip() {
        addProviderMappers();
        testSkip(NUM_ENTITIES*2);
    }

    @Test
    public void testAddProvidersOverwrite() {
        addProviders();
        testOverwrite();
    }

    @Test
    public void testAddProviderMappersOverwrite() {
        addProviderMappers();
        testOverwrite(NUM_ENTITIES*2);
    }
}
