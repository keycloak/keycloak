package org.keycloak.exportimport;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * Provider plugin interface for importing applications from an arbitrary configuration format
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ApplicationImporter extends Provider {
    public Object createJaxrsService(RealmModel realm, RealmAuth auth);
}
