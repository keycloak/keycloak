package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Map an assigned role to a different position and name in the token
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCRoleMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    public static final String ROLE_CONFIG = "role";
    public static String NEW_ROLE_NAME = "new.role.name";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(ROLE_CONFIG);
        property.setLabel("Role");
        property.setHelpText("Role name you want changed.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole");
        property.setType(ConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(NEW_ROLE_NAME);
        property.setLabel("New Role Name");
        property.setHelpText("The new role name.  The new name format corresponds to where in the access token the role will be mapped to.  So, a new name of 'myapp.newname' will map the role to that position in the access token.  A new name of 'newname' will map the role to the realm roles in the token.");
        property.setType(ConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-role-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Role Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map an assigned role to a new name or position in the token.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        String role = mappingModel.getConfig().get(ROLE_CONFIG);
        String newName = mappingModel.getConfig().get(NEW_ROLE_NAME);
        String appName = null;
        int scopeIndex = role.indexOf('.');
        if (scopeIndex > -1) {
            appName = role.substring(0, scopeIndex);
            AccessToken.Access access = token.getResourceAccess(appName);
            if (access == null) return token;

            role = role.substring(scopeIndex + 1);
            if (!access.getRoles().contains(role)) return token;
            access.getRoles().remove(role);
        } else {
            AccessToken.Access access = token.getRealmAccess();
            if (access == null) return token;
            access.getRoles().remove(role);
        }

        String newAppName = null;
        scopeIndex = newName.indexOf('.');
        if (scopeIndex > -1) {
            newAppName = role.substring(0, scopeIndex);
            newName = role.substring(scopeIndex + 1);
            token.addAccess(newAppName).addRole(newName);
        }
        return token;
    }
}
