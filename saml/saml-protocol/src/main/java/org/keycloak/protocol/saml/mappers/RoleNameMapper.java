package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.provider.ProviderConfigProperty;
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
public class RoleNameMapper extends AbstractOIDCProtocolMapper implements SAMLRoleNameMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ROLE_CONFIG = "role";
    public static String NEW_ROLE_NAME = "new.role.name";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ROLE_CONFIG);
        property.setLabel("Role");
        property.setHelpText("Role name you want changed.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(NEW_ROLE_NAME);
        property.setLabel("New Role Name");
        property.setHelpText("The new role name.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-role-name-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
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
        ClientModel app = null;
        if (container instanceof ClientModel) {
            app = ((ClientModel) container);
        }
        String role = model.getConfig().get(ROLE_CONFIG);
        String newName = model.getConfig().get(NEW_ROLE_NAME);
        String appName = null;
        int scopeIndex = role.indexOf('.');
        if (scopeIndex > -1) {
            if (app == null) return null;
            appName = role.substring(0, scopeIndex);
            if (!app.getClientId().equals(appName)) return null;
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
