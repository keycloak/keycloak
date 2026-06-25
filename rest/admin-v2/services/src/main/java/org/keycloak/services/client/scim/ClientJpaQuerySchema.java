package org.keycloak.services.client.scim;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

public final class ClientJpaQuerySchema implements ModelSchema<Model, ClientQueryRepresentation> {

    public static final ClientJpaQuerySchema INSTANCE = new ClientJpaQuerySchema();

    public static final Set<String> JPA_FIELDS = Set.of(
            "clientId", "enabled", "description", "displayName", "protocol", "appUrl");

    public static final List<ModelSchema<Model, ClientQueryRepresentation>> SCHEMAS = List.of(INSTANCE);

    private static final String SCHEMA_ID = "urn:keycloak:client-v2:query";

    private final Map<String, Attribute<Model, ClientQueryRepresentation>> attributes;

    private ClientJpaQuerySchema() {
        Map<String, Attribute<Model, ClientQueryRepresentation>> map = new LinkedHashMap<>();
        map.put("clientId", stringAttribute("clientId", "clientId"));
        map.put("enabled", booleanAttribute("enabled", "enabled"));
        map.put("description", stringAttribute("description", "description"));
        map.put("displayName", stringAttribute("displayName", "name"));
        map.put("protocol", stringAttribute("protocol", "protocol"));
        map.put("appUrl", stringAttribute("appUrl", "baseUrl"));
        this.attributes = Map.copyOf(map);
    }

    private static Attribute<Model, ClientQueryRepresentation> stringAttribute(String queryField, String entityField) {
        return Attribute.<Model, ClientQueryRepresentation>simple(queryField)
                .modelAttributeResolver(attribute -> entityField)
                .build()
                .get(0);
    }

    private static Attribute<Model, ClientQueryRepresentation> booleanAttribute(String queryField, String entityField) {
        return Attribute.<Model, ClientQueryRepresentation>simple(queryField)
                .modelAttributeResolver(attribute -> entityField)
                .bool()
                .build()
                .get(0);
    }

    @Override
    public String getId() {
        return SCHEMA_ID;
    }

    @Override
    public String getName() {
        return "ClientQuery";
    }

    @Override
    public String getDescription() {
        return "JPA-queryable client fields for Admin API v2";
    }

    @Override
    public Map<String, Attribute<Model, ClientQueryRepresentation>> getAttributes() {
        return attributes;
    }

    @Override
    public void populate(Model model, ClientQueryRepresentation representation) {
        throw new UnsupportedOperationException("Query schema does not support population");
    }

    @Override
    public void populate(ClientQueryRepresentation representation, Model model) {
        throw new UnsupportedOperationException("Query schema does not support population");
    }

    @Override
    public void validate(ClientQueryRepresentation representation) throws ModelValidationException {
        throw new UnsupportedOperationException("Query schema does not support validation");
    }

    @Override
    public Attribute<Model, ClientQueryRepresentation> getAttributeByPath(String path) {
        return attributes.get(path);
    }
}
