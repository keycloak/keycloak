package org.keycloak.scim.model.resourcetype.definition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.config.ServiceProviderConfig;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

/**
 * Manages custom SCIM resource type definitions stored as realm components and exposes the built-in resource
 * types as read-only definitions so both can be listed together.
 */
public class ScimResourceTypeStore {

    private static final String PROVIDER_TYPE = ScimResourceTypeDefinitionProvider.class.getName();

    /**
     * Built-in resource types that describe discovery endpoints rather than manageable resources.
     */
    private static final List<Class<? extends ResourceTypeRepresentation>> DISCOVERY_RESOURCE_TYPES =
            List.of(ServiceProviderConfig.class, ResourceType.class, Schema.class);

    private final KeycloakSession session;
    private final RealmModel realm;

    public ScimResourceTypeStore(KeycloakSession session) {
        this(session, session.getContext().getRealm());
    }

    public ScimResourceTypeStore(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    /**
     * Returns all custom definitions stored for the realm.
     */
    public Stream<ScimResourceTypeRepresentation> getCustomDefinitions() {
        return realm.getComponentsStream(realm.getId(), PROVIDER_TYPE)
                .map(ScimResourceTypeDefinitions::toRepresentation);
    }

    /**
     * Returns the built-in resource types (e.g. {@code User}, {@code Group}) as read-only definitions.
     */
    public Stream<ScimResourceTypeRepresentation> getBuiltInDefinitions() {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ScimResourceTypeProvider.class)
                .map(ScimResourceTypeProviderFactory.class::cast)
                .map(this::toBuiltInDefinition)
                .filter(Objects::nonNull);
    }

    /**
     * Returns the built-in definitions followed by the custom definitions.
     */
    public Stream<ScimResourceTypeRepresentation> getAllDefinitions() {
        return Stream.concat(getBuiltInDefinitions(), getCustomDefinitions());
    }

    /**
     * Returns a custom definition by its id, or {@code null} if there is no custom definition with that id.
     */
    public ScimResourceTypeRepresentation getById(String id) {
        ComponentModel model = getComponent(id);
        return model == null ? null : ScimResourceTypeDefinitions.toRepresentation(model);
    }

    /**
     * Creates a new custom definition. Validation is enforced by the component factory.
     */
    public ScimResourceTypeRepresentation create(ScimResourceTypeRepresentation definition) {
        ComponentModel model = new ComponentModel();

        model.setParentId(realm.getId());
        model.setProviderId(DefaultScimResourceTypeDefinitionProviderFactory.ID);
        model.setProviderType(PROVIDER_TYPE);
        ScimResourceTypeDefinitions.writeConfig(model, definition);

        model = realm.addComponentModel(model);

        return ScimResourceTypeDefinitions.toRepresentation(model);
    }

    /**
     * Updates an existing custom definition. Returns the updated definition, or {@code null} if no custom
     * definition with the given id exists.
     */
    public ScimResourceTypeRepresentation update(String id, ScimResourceTypeRepresentation definition) {
        ComponentModel model = getComponent(id);

        if (model == null) {
            return null;
        }

        ScimResourceTypeDefinitions.writeConfig(model, definition);
        realm.updateComponent(model);

        return ScimResourceTypeDefinitions.toRepresentation(model);
    }

    /**
     * Removes a custom definition. Returns {@code true} if a definition was removed.
     */
    public boolean delete(String id) {
        ComponentModel model = getComponent(id);

        if (model == null) {
            return false;
        }

        realm.removeComponent(model);

        return true;
    }

    private ComponentModel getComponent(String id) {
        if (id == null) {
            return null;
        }

        ComponentModel model = realm.getComponent(id);

        if (model == null || !PROVIDER_TYPE.equals(model.getProviderType())) {
            return null;
        }

        return model;
    }

    private ScimResourceTypeRepresentation toBuiltInDefinition(ScimResourceTypeProviderFactory<?> factory) {
        ScimResourceTypeProvider<?> provider = factory.create(session);

        if (DISCOVERY_RESOURCE_TYPES.contains(provider.getResourceType())) {
            return null;
        }

        ScimResourceTypeRepresentation definition = new ScimResourceTypeRepresentation();

        definition.setId(provider.getName());
        definition.setName(provider.getName());
        definition.setDescription(provider.getDescription());
        definition.setEndpoint("/" + factory.getId());
        definition.setSchema(provider.getSchema());
        definition.setBuiltIn(true);

        return definition;
    }
}
