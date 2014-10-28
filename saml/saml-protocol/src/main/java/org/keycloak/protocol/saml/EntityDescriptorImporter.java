package org.keycloak.protocol.saml;

import org.keycloak.exportimport.ApplicationImporter;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorImporter implements ApplicationImporter {
    @Override
    public Object createJaxrsService(RealmModel realm, RealmAuth auth) {
        return new EntityDescriptorImporterService(realm, auth);
    }

    @Override
    public void close() {

    }
}
