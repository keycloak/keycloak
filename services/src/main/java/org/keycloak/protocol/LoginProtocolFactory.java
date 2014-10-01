package org.keycloak.protocol;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocolFactory extends ProviderFactory<LoginProtocol> {
    Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager);
}
