package org.keycloak.tests.admin.partialimport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.IdentityProviderResource;
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
    public void testAddProvidersWithoutSignatureSettingsKeepsThemAbsent() {
        setFail();

        IdentityProviderRepresentation saml = new IdentityProviderRepresentation();
        saml.setAlias("saml-without-signature-settings");
        saml.setProviderId(SAMLIdentityProviderFactory.PROVIDER_ID);
        saml.setEnabled(true);
        Map<String, String> samlConfig = new HashMap<>();
        samlConfig.put(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "https://localhost/saml");
        saml.setConfig(samlConfig);

        piRep.setIdentityProviders(List.of(saml));

        PartialImportResults results = doImport();
        assertEquals(1, results.getAdded(), results.getErrorMessage());

        Map<String, String> storedSaml = managedRealm.admin().identityProviders().get(saml.getAlias()).toRepresentation().getConfig();

        assertFalse(storedSaml.containsKey(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
        assertFalse(storedSaml.containsKey(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED));
    }

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
