package org.keycloak.mappers;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapper extends Provider, ConfiguredProvider {

    /**
     *
     * @return factory, which created this mapper
     */
    UserFederationMapperFactory getFactory();
}
