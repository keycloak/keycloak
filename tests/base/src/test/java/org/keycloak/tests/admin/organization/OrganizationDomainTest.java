/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.admin.organization;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for organization domain management and identity provider linking.
 */
@KeycloakIntegrationTest
@DatabaseTest
public class OrganizationDomainTest {

    @InjectRealm(config = OrgRealmConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void enableOrganizations() {
        realm.updateWithCleanup(r -> r.organizationsEnabled(true));
    }

    private String createOrganization(String name, String... domainNames) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        for (String domainName : domainNames) {
            org.addDomain(new OrganizationDomainRepresentation(domainName));
        }
        String orgId = ApiUtil.getCreatedId(realm.admin().organizations().create(org));
        realm.cleanup().add(realmResource -> realmResource.organizations().get(orgId).delete());
        return orgId;
    }

    private IdentityProviderRepresentation createIdentityProvider(String alias) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(alias)
                .build();
        try (Response response = realm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        realm.cleanup().add(resource -> resource.identityProviders().get(alias).remove());
        return realm.admin().identityProviders().get(alias).toRepresentation();
    }


    @Test
    public void shouldListOrganizationDomain() {
        String orgId = createOrganization("organization1", "domain1.com", "domain2.com");
        OrganizationResource orgResource = realm.admin().organizations().get(orgId);

        // Fetch domains from the organization
        List<OrganizationDomainRepresentation> domains = orgResource.domains().getDomains();

        // Validate there are exactly two domains
        Assertions.assertEquals(2, domains.size(), "Organization should have exactly 2 domains");

        // Verify domain names
        Set<String> domainNames = domains.stream()
                .map(OrganizationDomainRepresentation::getName)
                .collect(Collectors.toSet());
        Assertions.assertTrue(domainNames.contains("domain1.com"), "Should contain domain1.com");
        Assertions.assertTrue(domainNames.contains("domain2.com"), "Should contain domain2.com");
    }
    @Test
    public void shouldUpdateDomainWithIdpLink() {
        // Create organization with three domains
        String orgId = createOrganization("organization2", "domain3.com", "domain4.com", "domain5.com");
        OrganizationResource orgResource = realm.admin().organizations().get(orgId);

        // Create identity provider
        String idpAlias = "testIdp";
        IdentityProviderRepresentation idp = createIdentityProvider(idpAlias);
        String idpId = idp.getInternalId();

        // Link the IDP to the organization
        try (Response response = orgResource.identityProviders().addIdentityProvider(idpAlias)) {
            Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Link only domain1 and domain2 to the IDP
        OrganizationDomainRepresentation domainUpdate = new OrganizationDomainRepresentation();
        domainUpdate.setIdpId(idpId);
        orgResource.domains().updateDomain("domain3.com", domainUpdate).close();
        orgResource.domains().updateDomain("domain4.com", domainUpdate).close();

        // Get domains by IDP
        List<OrganizationDomainRepresentation> linkedDomains = 
                orgResource.identityProviders().get(idpAlias).getDomains();

        // Verify only the two linked domains are returned
        Assertions.assertEquals(2, linkedDomains.size());
        List<String> domainNames = linkedDomains.stream()
                .map(OrganizationDomainRepresentation::getName)
                .toList();
        Assertions.assertTrue(domainNames.contains("domain3.com"));
        Assertions.assertTrue(domainNames.contains("domain4.com"));
        Assertions.assertFalse(domainNames.contains("domain5.com"));
    }

    public static class OrgRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.organizationsEnabled(false); // Will be enabled in @BeforeEach
        }
    }
}
