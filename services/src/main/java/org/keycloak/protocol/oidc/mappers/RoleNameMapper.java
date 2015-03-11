package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map an assigned role to a different position and name in the token
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleNameMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

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

    public static final String PROVIDER_ID = "oidc-role-name-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Role Name Mapper";
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

        String[] scopedRole = ProtocolMapperUtils.parseRole(role);
        String[] newScopedRole = ProtocolMapperUtils.parseRole(newName);
        String appName = scopedRole[0];
        String roleName = scopedRole[1];
        if (appName != null) {
            AccessToken.Access access = token.getResourceAccess(appName);
            if (access == null) return token;
            if (!access.getRoles().contains(roleName)) return token;
            access.getRoles().remove(roleName);
        } else {
            AccessToken.Access access = token.getRealmAccess();
            if (access == null) return token;
            access.getRoles().remove(roleName);
        }

        String newAppName = newScopedRole[0];
        String newRoleName = newScopedRole[1];
        AccessToken.Access access = null;
        if (newAppName == null) {
            access = token.getRealmAccess();
            if (access == null) {
                access = new AccessToken.Access();
                token.setRealmAccess(access);
            }
        } else {
            access = token.addAccess(newAppName);
        }
        access.addRole(newRoleName);
        return token;
    }

    public static ProtocolMapperModel create(String name,
                                             String role,
                                             String newName) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ROLE_CONFIG, role);
        config.put(NEW_ROLE_NAME, newName);
        mapper.setConfig(config);
        return mapper;

    }

}
