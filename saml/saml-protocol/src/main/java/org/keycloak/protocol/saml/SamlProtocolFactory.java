package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCUserModelMapper;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeBasicAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserModelUriReferenceAttributeStatementMapper;
import org.keycloak.services.managers.AuthenticationManager;
import org.picketlink.identity.federation.core.saml.v2.constants.X500SAMLProfileConstants;
import org.picketlink.identity.federation.core.sts.PicketLinkCoreSTS;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolFactory extends AbstractLoginProtocolFactory {

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new SamlService(realm, event, authManager);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        PicketLinkCoreSTS sts = PicketLinkCoreSTS.instance();
        sts.installDefaultConfiguration();
    }

    @Override
    public String getId() {
        return "saml";
    }

    @Override
    protected void addDefaults(RealmModel realm) {
       UserModelUriReferenceAttributeStatementMapper.addAttributeMapper(realm, "X500 email",
                "email",
                X500SAMLProfileConstants.EMAIL.get(), X500SAMLProfileConstants.EMAIL.getFriendlyName(),
                true, "email",
                false);
        UserModelUriReferenceAttributeStatementMapper.addAttributeMapper(realm, "X500 givenName",
                "firstName",
                X500SAMLProfileConstants.GIVEN_NAME.get(), X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(),
                true, "given name",
                false);
        UserModelUriReferenceAttributeStatementMapper.addAttributeMapper(realm, "X500 surname",
                "lastName",
                X500SAMLProfileConstants.SURNAME.get(), X500SAMLProfileConstants.SURNAME.getFriendlyName(),
                true, "family name",
                false);

    }

}
