package org.keycloak.protocol.oidc;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCFullNameMapper;
import org.keycloak.protocol.oidc.mappers.OIDCUserModelMapper;
import org.keycloak.services.managers.AuthenticationManager;

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
    protected void addDefaults(RealmModel realm) {
        int counter = 0;
        // the ids must never change!!!!  So if you add more default mappers, then add to end with higher counter.
        OIDCUserModelMapper.addClaimMapper(realm, "username",
                "username",
                "preferred_username", "String",
                true, "username",
                true);
        OIDCUserModelMapper.addClaimMapper(realm, "email",
                "email",
                "email", "String",
                true, "email",
                true);
        OIDCUserModelMapper.addClaimMapper(realm, "given name",
                "firstName",
                "given_name", "String",
                true, "given name",
                true);
        OIDCUserModelMapper.addClaimMapper(realm, "family name",
                "lastName",
                "family_name", "String",
                true, "family name",
                true);
        OIDCUserModelMapper.addClaimMapper(realm, "email verified",
                "emailVerified",
                "email_verified", "boolean",
                false, null,
                false);

        ProtocolMapperModel fullName = new ProtocolMapperModel();
        if (realm.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "full name") == null) {
            fullName.setName("full name");
            fullName.setProtocolMapper(OIDCFullNameMapper.PROVIDER_ID);
            fullName.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            fullName.setConsentRequired(true);
            fullName.setConsentText("full name");
            fullName.setAppliedByDefault(true);
            realm.addProtocolMapper(fullName);
        }

        ProtocolMapperModel address = new ProtocolMapperModel();
        if (realm.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "address") == null) {
            address.setName("address");
            address.setProtocolMapper(OIDCAddressMapper.PROVIDER_ID);
            address.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            address.setConsentRequired(true);
            address.setConsentText("address");
            address.setAppliedByDefault(false);
            realm.addProtocolMapper(address);
        }


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
