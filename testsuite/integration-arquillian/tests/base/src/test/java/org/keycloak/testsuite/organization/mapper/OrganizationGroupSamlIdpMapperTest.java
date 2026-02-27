/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.mapper;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedGroupMapper;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcSamlBrokerConfiguration;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

public class OrganizationGroupSamlIdpMapperTest extends AbstractOrganizationTest {

    @Override
    protected BrokerConfiguration createBrokerConfiguration() {
        return new KcSamlBrokerConfiguration() {
            @Override
            public RealmRepresentation createProviderRealm() {
                RealmRepresentation realmRep = super.createProviderRealm();
                realmRep.setOrganizationsEnabled(true);
                return realmRep;
            }

            @Override
            public String getIDPClientIdInProviderRealm() {
                return "saml-broker";
            }

            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation broker = super.setUpIdentityProvider(syncMode);
                broker.getConfig().put(SAMLIdentityProviderConfig.ENTITY_ID, getIDPClientIdInProviderRealm());
                return broker;
            }
        };
    }

    @Test
    public void testHardcodedGroupMapperAssignsOrganizationGroupMembershipWithSamlIdp() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("saml-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // Create HardcodedGroupMapper pointing to the org group
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("saml-hardcoded-group-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Authenticate via SAML IdP - user should be added to the org group via the mapper
        assertBrokerRegistration(orgResource, bc.getUserLogin(), bc.getUserEmail());

        UserRepresentation user = getUserRepresentation(bc.getUserEmail());
        assertNotNull(user);

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));
        assertThat(groupMembers.get(0).getId(), is(user.getId()));
    }
}
