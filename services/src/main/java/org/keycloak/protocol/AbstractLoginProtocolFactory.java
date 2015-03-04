package org.keycloak.protocol;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
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
        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.ClientCreationEvent) {
                    ClientModel client = ((RealmModel.ClientCreationEvent)event).getCreatedClient();
                    addDefaults(client);
                }
            }
        });


    }

    protected abstract void addDefaults(ClientModel realm);

    @Override
    public void close() {

    }
}
