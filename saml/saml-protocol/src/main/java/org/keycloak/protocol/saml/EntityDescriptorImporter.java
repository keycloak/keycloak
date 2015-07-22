package org.keycloak.protocol.saml;

import org.keycloak.exportimport.ClientImporter;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorImporter implements ClientImporter {
    @Override
    public Object createJaxrsService(RealmModel realm, RealmAuth auth) {
        return new EntityDescriptorImporterService(realm, auth);
    }

    @Override
    public void close() {

    }
}
