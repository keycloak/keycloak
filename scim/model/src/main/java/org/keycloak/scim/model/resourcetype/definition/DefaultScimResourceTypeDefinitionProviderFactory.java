package org.keycloak.scim.model.resourcetype.definition;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

/**
 * Default implementation storing custom SCIM resource type definitions as realm components.
 */
public class DefaultScimResourceTypeDefinitionProviderFactory implements ScimResourceTypeDefinitionProviderFactory {

    public static final String ID = "default";

    @Override
    public ScimResourceTypeDefinitionProvider create(KeycloakSession session, ComponentModel model) {
        return new DefaultScimResourceTypeDefinitionProvider(ScimResourceTypeDefinitions.toRepresentation(model));
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        ScimResourceTypeRepresentation definition = ScimResourceTypeDefinitions.toRepresentation(model);

        ScimResourceTypeDefinitions.validate(definition);

        String name = definition.getName();
        String endpoint = ScimResourceTypeDefinitions.resolveEndpoint(definition);

        // A custom type name must not clash with a built-in SCIM resource type, since the type name is used to
        // route requests under /scim/v2/{resourceType}.
        if (getBuiltInResourceTypeNames(session).stream().anyMatch(name::equalsIgnoreCase)) {
            throw new ComponentValidationException("Resource type name '" + name + "' is reserved by a built-in resource type");
        }

        List<ComponentModel> others = realm.getComponentsStream(realm.getId(), ScimResourceTypeDefinitionProvider.class.getName())
                .filter(other -> !Objects.equals(other.getId(), model.getId()))
                .collect(Collectors.toList());

        for (ComponentModel other : others) {
            ScimResourceTypeRepresentation existing = ScimResourceTypeDefinitions.toRepresentation(other);

            if (name.equalsIgnoreCase(existing.getName())) {
                throw new ComponentValidationException("A resource type with name '" + name + "' already exists");
            }

            if (endpoint.equalsIgnoreCase(ScimResourceTypeDefinitions.resolveEndpoint(existing))) {
                throw new ComponentValidationException("A resource type with endpoint '" + endpoint + "' already exists");
            }
        }
    }

    private Set<String> getBuiltInResourceTypeNames(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ScimResourceTypeProvider.class)
                .map(ScimResourceTypeProviderFactory.class::cast)
                .map(ScimResourceTypeProviderFactory::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(ScimResourceTypeDefinitions.CONFIG_DESCRIPTION)
                .label("Description")
                .helpText("A human-readable description of the resource type.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(ScimResourceTypeDefinitions.CONFIG_ENDPOINT)
                .label("Endpoint")
                .helpText("The relative endpoint of the resource type, e.g. /Devices. Defaults to /<name>.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(ScimResourceTypeDefinitions.CONFIG_SCHEMA)
                .label("Schema")
                .helpText("The primary schema URN of the resource type. A default is derived from the name when omitted.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(ScimResourceTypeDefinitions.CONFIG_ATTRIBUTES)
                .label("Attributes")
                .helpText("The attribute definitions of the resource type, serialized as a JSON array.")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .add()
                .build();
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
        return ID;
    }
}
