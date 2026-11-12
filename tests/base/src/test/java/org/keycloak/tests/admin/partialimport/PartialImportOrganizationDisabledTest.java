package org.keycloak.tests.admin.partialimport;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = PartialImportOrganizationDisabledTest.OrganizationDisabledServerConfig.class)
public class PartialImportOrganizationDisabledTest extends AbstractPartialImportTest {

    @Test
    public void testOrganizationsAreIgnored() {
        setFail();
        addGroups(1);
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName("Acme Corp");
        organization.setAlias("acme");
        piRep.setOrganizations(List.of(organization));

        PartialImportResults results = doImport();

        assertEquals(1, results.getAdded(), results.getErrorMessage());
    }

    public static class OrganizationDisabledServerConfig extends PartialImportServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return super.configure(builder).featuresDisabled(Profile.Feature.ORGANIZATION);
        }
    }
}
