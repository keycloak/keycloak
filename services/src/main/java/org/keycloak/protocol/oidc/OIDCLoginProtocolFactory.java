package org.keycloak.protocol.oidc;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCAddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCFullNameMapper;
import org.keycloak.protocol.oidc.mappers.OIDCUserModelMapper;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory implements LoginProtocolFactory {
    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakSession session = factory.create();
        session.getTransaction().begin();
        try {
            List<RealmModel> realms = session.realms().getRealms();
            for (RealmModel realm : realms) addMappers(realm);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
        } finally {
            session.close();
        }

        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.RealmCreationEvent) {
                    RealmModel realm = ((RealmModel.RealmCreationEvent)event).getCreatedRealm();
                    addMappers(realm);
                }
            }
        });


    }

    protected void addMappers(RealmModel realm) {
        int counter = 0;
        // the ids must never change!!!!  So if you add more default mappers, then add to end with higher counter.
        addClaimMapper(realm, "username", OIDCUserModelMapper.PROVIDER_ID,
                OIDCUserModelMapper.USER_MODEL_PROPERTY, "username",
                "preferred_username", "String",
                true, "username",
                true);
        addClaimMapper(realm, "email", OIDCUserModelMapper.PROVIDER_ID,
                OIDCUserModelMapper.USER_MODEL_PROPERTY, "email",
                "email", "String",
                true, "email",
                true);
        addClaimMapper(realm, "given name", OIDCUserModelMapper.PROVIDER_ID,
                OIDCUserModelMapper.USER_MODEL_PROPERTY, "firstName",
                "given_name", "String",
                true, "given name",
                true);
        addClaimMapper(realm, "family name", OIDCUserModelMapper.PROVIDER_ID,
                OIDCUserModelMapper.USER_MODEL_PROPERTY, "lastName",
                "family_name", "String",
                true, "family name",
                true);
        addClaimMapper(realm, "email verified", OIDCUserModelMapper.PROVIDER_ID,
                OIDCUserModelMapper.USER_MODEL_PROPERTY, "emailVerified",
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

    protected void addClaimMapper(RealmModel realm, String name, String mapperRef,
                                  String propertyName, String propertyNameValue,
                                  String tokenClaimName, String claimType,
                                  boolean consentRequired, String consentText,
                                  boolean appliedByDefault) {
        ProtocolMapperModel mapper = realm.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, name);
        if (mapper != null) return;
        mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperRef);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        mapper.setAppliedByDefault(appliedByDefault);
        Map<String, String> config = new HashMap<String, String>();
        config.put(propertyName, propertyNameValue);
        config.put(AttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(AttributeMapperHelper.JSON_TYPE, claimType);
        mapper.setConfig(config);
        realm.addProtocolMapper(mapper);
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new OIDCLoginProtocolService(realm, event, authManager);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "openid-connect";
    }
}
