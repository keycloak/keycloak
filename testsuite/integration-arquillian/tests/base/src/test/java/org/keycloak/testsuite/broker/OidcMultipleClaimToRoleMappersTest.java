/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.oidc.mappers.ClaimToRoleMapper;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * Runs the same tests as {@link OidcClaimToRoleMapperTest} but using multiple OIDC mappers that map different IDP claims
 * to the same {@code Keycloak} role.
 * <p/>
 * This class aims to test the fix for {@code KEYCLOAK-8730}. When configuring two or more mappers that map different IDP
 * attributes to the same {@code Keycloak} role, the user would sometimes not be granted the expected {@code Keycloak} role
 * depending on the order in which the mappers would run. For example, consider a scenario where mapper A maps IDP role 'x'
 * to the role 'keycloak' and mapper B maps IDP role 'y' to the same role 'keycloak'. The user only has role 'x' in the IDP,
 * so when updating the brokered user the following could happen:
 * <ul>
 *     <li>mapper A runs, checks user has role 'x', therefore role 'keycloak' is granted to user</li>
 *     <li>mapper B runs, checks users doesn't have role 'y', so it removes role 'keycloak' from user even if the previous
 *     mapper has already verified that the role should have been granted.</li>
 * </ul>
 * This test configures three different OIDC claim mappers that all map to the same {@code Keycloak} role. Only the first
 * mapper actually succeeds in applying the mapping, the other two do nothing as the test user doesn't have the necessary
 * role/attribute(s). The test then verifies that the user still contains the mapped role after all mappers run.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class OidcMultipleClaimToRoleMappersTest extends OidcClaimToRoleMapperTest {

    private static final String CLAIMS_OR_ATTRIBUTES = "[\n" +
            "  {\n" +
            "    \"key\": \"test attribute\",\n" +
            "    \"value\": \"test value*\"\n" +
            "  }\n" +
            "]";

    @Override
    protected void createClaimToRoleMapper(IdentityProviderRepresentation idp, String claimValue, IdentityProviderMapperSyncMode syncMode) {
        // first mapper that maps attributes the user has - it should perform the mapping to the expected role.
        IdentityProviderMapperRepresentation firstOidcClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        firstOidcClaimToRoleMapper.setName("claim-to-role-mapper");
        firstOidcClaimToRoleMapper.setIdentityProviderMapper(ClaimToRoleMapper.PROVIDER_ID);
        firstOidcClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(ClaimToRoleMapper.CLAIM, OidcClaimToRoleMapperTest.CLAIM)
                .put(ClaimToRoleMapper.CLAIM_VALUE, claimValue)
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        firstOidcClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(firstOidcClaimToRoleMapper).close();

        // second mapper that maps an external role claim the test user doesn't have - it would normally end up removing the
        // mapped role but it should now check if a previous mapper has already granted the same role.
        IdentityProviderMapperRepresentation secondOidcClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        secondOidcClaimToRoleMapper.setName("external-keycloak-role-mapper");
        secondOidcClaimToRoleMapper.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        secondOidcClaimToRoleMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", "missing-role")
                .put("role", CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());
        secondOidcClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(secondOidcClaimToRoleMapper).close();

        // third mapper (advanced) that maps a claim the test user doesn't have - it would normally end up removing the
        // mapped role but it should now check if a previous mapper has already granted the same role.
        IdentityProviderMapperRepresentation thirdOidcClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        thirdOidcClaimToRoleMapper.setName("advanced-claim-to-role-mapper");
        thirdOidcClaimToRoleMapper.setIdentityProviderMapper(AdvancedClaimToRoleMapper.PROVIDER_ID);
        thirdOidcClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedClaimToRoleMapper.CLAIM_PROPERTY_NAME, CLAIMS_OR_ATTRIBUTES)
                .put(AdvancedClaimToRoleMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME, Boolean.TRUE.toString())
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        thirdOidcClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(thirdOidcClaimToRoleMapper).close();
    }
}
