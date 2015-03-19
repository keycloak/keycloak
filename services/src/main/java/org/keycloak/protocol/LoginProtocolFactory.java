package org.keycloak.protocol;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocolFactory extends ProviderFactory<LoginProtocol> {
    /**
     * List of built in protocol mappers that can be used to apply to clients.
     *
     * @return
     */
    List<ProtocolMapperModel> getBuiltinMappers();

    /**
     * List of mappers, which are added to new clients by default
     * @return
     */
    List<ProtocolMapperModel> getDefaultBuiltinMappers();

    Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager);
}
