package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.exportimport.ApplicationImporter;
import org.keycloak.exportimport.ApplicationImporterFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorImporterFactory implements ApplicationImporterFactory {
    @Override
    public String getDisplayName() {
        return "SAML 2.0 Entity Descriptor";
    }

    @Override
    public ApplicationImporter create(KeycloakSession session) {
        return new EntityDescriptorImporter();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "saml2-entity-descriptor";
    }
}
