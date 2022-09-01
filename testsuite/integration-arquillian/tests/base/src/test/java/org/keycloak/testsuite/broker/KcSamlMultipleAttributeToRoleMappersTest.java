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
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.mappers.AdvancedAttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * Runs the same tests as {@link AttributeToRoleMapperTest} but using multiple SAML mappers that map different IDP attributes
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
 * This test configures three different SAML attribute mappers that all map to the same {@code Keycloak} role. Only the first
 * mapper actually succeeds in applying the mapping, the other two do nothing as the test user doesn't have the necessary
 * role/attribute(s). The test then verifies that the user still contains the mapped role after all mappers run.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class KcSamlMultipleAttributeToRoleMappersTest extends AttributeToRoleMapperTest {

    private static final String ATTRIBUTES_TO_MATCH = "[\n" +
            "  {\n" +
            "    \"key\": \"test attribute\",\n" +
            "    \"value\": \"test value\"\n" +
            "  }\n" +
            "]";

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        // first mapper that maps a role the test user has - it should perform the mapping.
        IdentityProviderMapperRepresentation firstSamlAttributeToRoleMapper = new IdentityProviderMapperRepresentation();
        firstSamlAttributeToRoleMapper.setName("first-role-mapper");
        firstSamlAttributeToRoleMapper.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        firstSamlAttributeToRoleMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(UserAttributeMapper.ATTRIBUTE_NAME, "Role")
                .put(ATTRIBUTE_VALUE, ROLE_USER)
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        firstSamlAttributeToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(firstSamlAttributeToRoleMapper).close();

        // second mapper that maps a role the test user doesn't have - it would normally end up removing the mapped role but
        // it should now check if a previous mapper has already granted the same mapped role.
        IdentityProviderMapperRepresentation secondSamlAttributeToRoleMapper = new IdentityProviderMapperRepresentation();
        secondSamlAttributeToRoleMapper.setName("second-role-mapper");
        secondSamlAttributeToRoleMapper.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        secondSamlAttributeToRoleMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(UserAttributeMapper.ATTRIBUTE_NAME, "Role")
                .put(ATTRIBUTE_VALUE, "missing-role")
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        secondSamlAttributeToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(secondSamlAttributeToRoleMapper).close();

        // third mapper (advanced) that maps an attribute the test user doesn't have - it would normally end up removing the
        // mapped role but it should now check if a previous mapper has already granted the same role.
        IdentityProviderMapperRepresentation thirdSamlAttributeToRoleMapper = new IdentityProviderMapperRepresentation();
        thirdSamlAttributeToRoleMapper.setName("advanced-role-mapper");
        thirdSamlAttributeToRoleMapper.setIdentityProviderMapper(AdvancedAttributeToRoleMapper.PROVIDER_ID);
        thirdSamlAttributeToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedAttributeToRoleMapper.ATTRIBUTE_PROPERTY_NAME, ATTRIBUTES_TO_MATCH)
                .put(AdvancedAttributeToRoleMapper.ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME, Boolean.FALSE.toString())
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());
        thirdSamlAttributeToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(thirdSamlAttributeToRoleMapper).close();
    }
}
