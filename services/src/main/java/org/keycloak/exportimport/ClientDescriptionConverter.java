package org.keycloak.exportimport;

import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Provider plugin interface for importing clients from an arbitrary configuration format
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientDescriptionConverter extends Provider {

    ClientRepresentation convertToInternal(String description);

}
