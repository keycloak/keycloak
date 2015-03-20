package org.keycloak.protocol.oidc;

import org.keycloak.constants.KerberosConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
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

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String FULL_NAME = "full name";

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {

        ProtocolMapperModel model;
        model = UserPropertyMapper.createClaimMapper(USERNAME,
                "username",
                "preferred_username", "String",
                true, USERNAME,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(EMAIL,
                "email",
                "email", "String",
                true, EMAIL,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(GIVEN_NAME,
                "firstName",
                "given_name", "String",
                true, GIVEN_NAME,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(FAMILY_NAME,
                "lastName",
                "family_name", "String",
                true, FAMILY_NAME,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(EMAIL_VERIFIED,
                "emailVerified",
                "email_verified", "boolean",
                false, null,
                true, true);
        builtins.add(model);

        ProtocolMapperModel fullName = new ProtocolMapperModel();
        fullName.setName(FULL_NAME);
        fullName.setProtocolMapper(FullNameMapper.PROVIDER_ID);
        fullName.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        fullName.setConsentRequired(true);
        fullName.setConsentText(FULL_NAME);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        fullName.setConfig(config);
        builtins.add(fullName);
        defaultBuiltins.add(fullName);

        ProtocolMapperModel address = AddressMapper.createAddressMapper();
        builtins.add(address);

        model = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
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
