package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.saml.SamlProtocol;

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
public class SAMLBasicRoleNameMapper extends AbstractOIDCProtocolMapper implements SAMLRoleNameMapper {

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
        property.setHelpText("The new role name.");
        property.setType(ConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-role-name-mapper";


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
        return "Role Mapper";

    }

    @Override
    public String getHelpText() {
        return "Map an assigned role to a new name";
    }

    @Override
    public String mapName(ProtocolMapperModel model, RoleModel roleModel) {
        RoleContainerModel container = roleModel.getContainer();
        ApplicationModel app = null;
        if (container instanceof ApplicationModel) {
            app = ((ApplicationModel) container);
        }
        String role = model.getConfig().get(ROLE_CONFIG);
        String newName = model.getConfig().get(NEW_ROLE_NAME);
        String appName = null;
        int scopeIndex = role.indexOf('.');
        if (scopeIndex > -1) {
            if (app == null) return null;
            appName = role.substring(0, scopeIndex);
            if (!app.getName().equals(appName)) return null;
            role = role.substring(scopeIndex + 1);
        } else {
            if (app != null) return null;
        }
        if (roleModel.getName().equals(role)) return newName;
        return null;
   }

    public static ProtocolMapperModel create(String name,
                                             String role,
                                             String newName) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ROLE_CONFIG, role);
        config.put(NEW_ROLE_NAME, newName);
        mapper.setConfig(config);
        return mapper;

    }

}
