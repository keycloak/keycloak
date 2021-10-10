/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExternalKeycloakRoleToRoleMapper extends AbstractClaimToRoleMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final String EXTERNAL_ROLE = "external.role";
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(EXTERNAL_ROLE);
        property1.setLabel("External role");
        property1.setHelpText("External role to check for.  To reference a client role the syntax is clientname.clientrole, i.e. myclient.myrole");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(ConfigConstants.ROLE);
        property.setLabel("Role");
        property.setHelpText("Role to grant to user if external role is present.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference a client role the syntax is clientname.clientrole, i.e. myclient.myrole");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "keycloak-oidc-role-to-role-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Role Importer";
    }

    @Override
    public String getDisplayType() {
        return "External Role to Role";
    }

    @Override
    protected boolean applies(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        JsonWebToken token = (JsonWebToken)context.getContextData().get(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN);
        String[] parseRole = KeycloakModelUtils.parseRole(mapperModel.getConfig().get(EXTERNAL_ROLE));
        String externalRoleName = parseRole[1];
        String claimName = parseRole[0] == null ? "realm_access.roles" : "resource_access." + parseRole[0] + ".roles";
        Object claim = getClaimValue(token, claimName);
        return valueEquals(externalRoleName, claim);
    }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // The legacy mapper actually did nothing although it pretended to do something
    }

    @Override
    public String getHelpText() {
        return "Looks for an external role in a keycloak access token.  If external role exists, grant the user the specified realm or client role.";
    }

}
