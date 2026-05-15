package org.keycloak.protocol.oidc.mappers;

/**
 * Marker interface for protocol mappers that handle organization claims.
 *
 * <p>Mappers implementing this interface are recognized by
 * {@link org.keycloak.organization.protocol.mappers.oidc.OrganizationScope OrganizationScope}
 * as organization-aware, enabling scope-based organization resolution for the
 * client scope containing the mapper.
 *
 * <p>This follows the same pattern as {@link OIDCAccessTokenMapper} and
 * {@link UserInfoTokenMapper}, where mapper capabilities are declared
 * through interface implementation and checked via {@code instanceof}.
 */
public interface OrganizationAwareMapper {
}
