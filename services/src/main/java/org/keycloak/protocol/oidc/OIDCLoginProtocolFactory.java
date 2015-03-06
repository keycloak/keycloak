package org.keycloak.protocol.oidc;

import org.keycloak.constants.KerberosConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCFullNameMapper;
import org.keycloak.protocol.oidc.mappers.OIDCUserModelMapper;
import org.keycloak.protocol.oidc.mappers.OIDCUserSessionNoteMapper;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory extends AbstractLoginProtocolFactory {
    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {

        ProtocolMapperModel model;
        model = OIDCUserModelMapper.createClaimMapper("username",
                "username",
                "preferred_username", "String",
                true, "username",
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserModelMapper.createClaimMapper("email",
                "email",
                "email", "String",
                true, "email",
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserModelMapper.createClaimMapper("given name",
                "firstName",
                "given_name", "String",
                true, "given name",
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserModelMapper.createClaimMapper("family name",
                "lastName",
                "family_name", "String",
                true, "family name",
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserModelMapper.createClaimMapper("email verified",
                "emailVerified",
                "email_verified", "boolean",
                false, null,
                true, true);
        builtins.add(model);

        ProtocolMapperModel fullName = new ProtocolMapperModel();
        fullName.setName("full name");
        fullName.setProtocolMapper(OIDCFullNameMapper.PROVIDER_ID);
        fullName.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        fullName.setConsentRequired(true);
        fullName.setConsentText("full name");
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        fullName.setConfig(config);
        builtins.add(fullName);
        defaultBuiltins.add(fullName);

        ProtocolMapperModel address = OIDCAddressMapper.createAddressMapper();
        builtins.add(address);

        model = OIDCUserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                true, false);
        builtins.add(model);
    }

    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) client.addProtocolMapper(model);
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new OIDCLoginProtocolService(realm, event, authManager);
    }

    @Override
    public String getId() {
        return "openid-connect";
    }
}
