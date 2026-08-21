package org.keycloak.services.client.scim;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.keycloak.models.Model;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.scim.model.filter.ScimAttributeJpaExpressionResolver;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;

public class ClientResourceTypeProvider implements ScimResourceTypeProvider<BaseClientRepresentation>, ScimAttributeJpaExpressionResolver{

    @Override
    public void close() {
        // v2 may not need these classes to be Providers
    }

    @Override
    public String getSchema() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Model> List<ModelSchema<M, BaseClientRepresentation>> getSchemas() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<BaseClientRepresentation> getResourceType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseClientRepresentation create(BaseClientRepresentation resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseClientRepresentation update(BaseClientRepresentation resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseClientRepresentation get(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<BaseClientRepresentation> getAll(SearchRequest searchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(SearchRequest searchRequest, int resourceSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root,
            BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver) {
        if ("roles".equals(attribute.getName())) {
            Join<?, ?> join = joinResolver.apply(RoleEntity.class, () -> root.join(RoleEntity.class));
            join.on(cb.equal(root.get("id"), join.get("clientId")));
            return join.get("name");
        } else if ("auth.method".equals(attribute.getName())) {
            return cb.selectCase().when(cb.and(cb.equal(root.get("protocol"), OIDCClientRepresentation.PROTOCOL),
                    cb.isFalse(root.get("publicClient"))), root.get("clientAuthenticatorType"));
        }
        return null;
    }

}
