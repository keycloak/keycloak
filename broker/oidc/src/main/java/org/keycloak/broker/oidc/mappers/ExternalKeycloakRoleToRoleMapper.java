package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExternalKeycloakRoleToRoleMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final String EXTERNAL_ROLE = "external.role";

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(EXTERNAL_ROLE);
        property1.setLabel("External role");
        property1.setHelpText("External role to check for.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(HardcodedRoleMapper.ROLE);
        property.setLabel("Role");
        property.setHelpText("Role to grant to user if external role is present.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "keycloak-oidc-role-to-role-idp-mapper";


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
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = hasRole(realm, mapperModel, context);
        if (role != null) {
            user.grantRole(role);
        }
    }

    private RoleModel hasRole(RealmModel realm,IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        JsonWebToken token = (JsonWebToken)context.getContextData().get(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN);
        //if (token == null) return;
        String roleName = mapperModel.getConfig().get(HardcodedRoleMapper.ROLE);
        String[] parseRole = HardcodedRoleMapper.parseRole(mapperModel.getConfig().get(EXTERNAL_ROLE));
        String externalRoleName = parseRole[1];
        String claimName = null;
        if (parseRole[0] == null) {
            claimName = "realm_access.roles";
        } else {
            claimName = "resource_access." + parseRole[0] + ".roles";
        }
        Object claim = getClaimValue(token, claimName);
        if (valueEquals(externalRoleName, claim)) {
            RoleModel role = HardcodedRoleMapper.getRoleFromString(realm, roleName);
            if (role == null) throw new IdentityBrokerException("Unable to find role: " + roleName);
            return role;
        }
        return null;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = hasRole(realm, mapperModel, context);
        if (role == null) {
            user.deleteRoleMapping(role);
        }
    }

    @Override
    public String getHelpText() {
        return "Looks for an external role in a keycloak access token.  If external role exists, grant the user the specified realm or application role.";
    }

}
