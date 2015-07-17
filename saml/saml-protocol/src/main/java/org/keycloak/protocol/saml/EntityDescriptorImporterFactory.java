package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.exportimport.ClientImporter;
import org.keycloak.exportimport.ClientImporterFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorImporterFactory implements ClientImporterFactory {
    @Override
    public String getDisplayName() {
        return "SAML 2.0 Entity Descriptor";
    }

    @Override
    public ClientImporter create(KeycloakSession session) {
        return new EntityDescriptorImporter();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "saml2-entity-descriptor";
    }
}
