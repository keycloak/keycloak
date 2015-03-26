package org.keycloak.migration;

import java.util.List;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * Various common utils needed for migration from older version to newer
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MigrationProvider extends Provider {

    /**
     * @param claimMask mask used on ClientModel in 1.1.0
     * @return set of 1.2.0.Beta1 protocol mappers corresponding to given claimMask
     */
    List<ProtocolMapperRepresentation> getMappersForClaimMask(Long claimMask);

}
