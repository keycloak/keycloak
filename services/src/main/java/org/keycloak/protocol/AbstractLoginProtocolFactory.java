package org.keycloak.protocol;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractLoginProtocolFactory implements LoginProtocolFactory {

    private static final Logger logger = Logger.getLogger(AbstractLoginProtocolFactory.class);

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakSession session = factory.create();
        session.getTransaction().begin();
        try {
            List<RealmModel> realms = session.realms().getRealms();
            for (RealmModel realm : realms) addDefaults(realm);
            session.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Can't add default mappers to realm", e);
            session.getTransaction().rollback();
        } finally {
            session.close();
        }

        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.RealmCreationEvent) {
                    RealmModel realm = ((RealmModel.RealmCreationEvent)event).getCreatedRealm();
                    addDefaults(realm);
                }
            }
        });


    }

    protected abstract void addDefaults(RealmModel realm);

    @Override
    public void close() {

    }
}
