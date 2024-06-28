package org.keycloak.protocol.saml.mappers;

import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.models.IdentityProviderMapperModel;

public interface SamlMetadataDescriptorUpdater
{
    void updateMetadata(IdentityProviderMapperModel mapperModel, EntityDescriptorType descriptor);
}