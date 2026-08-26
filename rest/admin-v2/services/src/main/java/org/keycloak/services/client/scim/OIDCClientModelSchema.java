package org.keycloak.services.client.scim;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Schema singleton for OIDC clients and provides attribute-filtered
 * population of {@link OIDCClientRepresentation}.
 */
public final class OIDCClientModelSchema extends BaseClientModelSchema<OIDCClientRepresentation> {

    public static final OIDCClientModelSchema INSTANCE = new OIDCClientModelSchema();

    /**
     * Sub-attribute {@code auth.method} built as a proper complex attribute so that
     * {@link org.keycloak.scim.model.filter.ScimJPAPredicateProvider} can build a JPA predicate
     * against {@code clientAuthenticatorType} when filtering by {@code auth.method}.
     *
     * <p>TODO: It is intentionally absent from {@link #getAttributes()} to avoid interfering with the
     * whole-object {@code auth} population logic; it is exposed only through
     * {@link #getAttributeByPath(String)}.
     */
    private static final Attribute<ClientModel, OIDCClientRepresentation> AUTH_METHOD_ATTR;

    static {
        List<Attribute<ClientModel, OIDCClientRepresentation>> subAttrs =
                Attribute.<ClientModel, OIDCClientRepresentation>complex("auth", OIDCClientRepresentation.Auth.class)
                        .modelAttributeResolver(a -> "") // TODO: a dummy mapping allows the more complex mapping logic from ClientResourceTypeProvider to be used
                        // TODO: withAttribute forces the use of the lower function because the Attribute is marked as !caseExact and !storedLowerCase
                        .withAttribute("method",
                                (model, name, value) -> model.setClientAuthenticatorType(value))
                        .build();
        // build() returns only sub-attributes when withAttribute() is used; the single entry is auth.method
        AUTH_METHOD_ATTR = subAttrs.get(0);
    }

    private OIDCClientModelSchema() {
    }

    @Override
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, OIDCClientRepresentation>> map) {
        map.put("loginFlows",          multivaluedStringAttr("loginFlows",          null,           (rep, v) -> rep.setLoginFlows(toFlowSet(v)), (BiConsumer<ClientModel, Set<OIDCClientRepresentation.Flow>>) (model, flows) -> setModelFromFlows(flows, model)));
        map.put("auth",                customAttr           ("auth",                null,           OIDCClientRepresentation::setAuth,           (model, auth) -> setAuth(model, (OIDCClientRepresentation.Auth) auth)));
        map.put("webOrigins",          multivaluedStringAttr("webOrigins",          "webOrigins",   (rep, v) -> rep.setWebOrigins(v),            (BiConsumer<ClientModel, Set<String>>) (model, origins) -> model.setWebOrigins(origins != null ? new LinkedHashSet<>(origins) : null)));
        map.put("serviceAccountRoles", multivaluedStringAttr("serviceAccountRoles", null,           (rep, v) -> rep.setServiceAccountRoles(v),  null));
    }

    /**
     * Extends the base lookup to also expose {@code auth.method} as a queryable complex
     * sub-attribute, even though it is not part of the attributes map (to keep the whole-object
     * {@code auth} population logic intact).
     */
    @Override
    public Attribute<ClientModel, OIDCClientRepresentation> getAttributeByPath(String path) {
        if ("auth.method".equals(path)) {
            return AUTH_METHOD_ATTR;
        }
        return super.getAttributeByPath(path);
    }

    @Override
    protected Object getAttributeValue(ClientModel model, String name) {
        return switch (name) {
            case "loginFlows"          -> getLoginFlowNames(model);
            case "auth"                -> getAuth(model);
            case "webOrigins"          -> new LinkedHashSet<>(model.getWebOrigins());
            case "serviceAccountRoles" -> getServiceAccountRoles(model);
            default                    -> super.getAttributeValue(model, name);
        };
    }

    @Override
    public Object getRepresentationValue(OIDCClientRepresentation rep, String name) {
        return switch (name) {
            case "loginFlows"          -> rep.getLoginFlows();
            case "auth"                -> rep.getAuth();
            case "auth.method"         -> rep.getAuth() != null ? rep.getAuth().getMethod() : null;
            case "webOrigins"          -> rep.getWebOrigins();
            case "serviceAccountRoles" -> rep.getServiceAccountRoles();
            default                    -> super.getRepresentationValue(rep, name);
        };
    }

    private OIDCClientRepresentation.Auth getAuth(ClientModel model) {
        if (!model.isPublicClient()) {
            OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
            auth.setMethod(model.getClientAuthenticatorType());
            auth.setSecret(model.getSecret());
            return auth;
        }
        return null;
    }

    private void setAuth(ClientModel model, OIDCClientRepresentation.Auth auth) {
        if (auth != null) {
            model.setPublicClient(false);
            model.setClientAuthenticatorType(auth.getMethod());
            model.setSecret(auth.getSecret());
        } else {
            model.setPublicClient(true);
        }
    }

    private void setModelFromFlows(Set<OIDCClientRepresentation.Flow> flows, ClientModel model) {
        if (flows != null) {
            model.setStandardFlowEnabled(flows.contains(OIDCClientRepresentation.Flow.STANDARD));
            model.setImplicitFlowEnabled(flows.contains(OIDCClientRepresentation.Flow.IMPLICIT));
            model.setDirectAccessGrantsEnabled(flows.contains(OIDCClientRepresentation.Flow.DIRECT_GRANT));
        }
    }

    private Set<String> getLoginFlowNames(ClientModel model) {
        Set<String> flows = new HashSet<>();
        if (model.isStandardFlowEnabled())        flows.add(OIDCClientRepresentation.Flow.STANDARD.name());
        if (model.isImplicitFlowEnabled())         flows.add(OIDCClientRepresentation.Flow.IMPLICIT.name());
        if (model.isDirectAccessGrantsEnabled())   flows.add(OIDCClientRepresentation.Flow.DIRECT_GRANT.name());
        if (model.isServiceAccountsEnabled())      flows.add(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT.name());
        return flows;
    }

    private static Set<OIDCClientRepresentation.Flow> toFlowSet(Set<String> names) {
        if (names == null) return new LinkedHashSet<>();
        return names.stream()
                .map(OIDCClientRepresentation.Flow::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> getServiceAccountRoles(ClientModel model) {
        if (model.isServiceAccountsEnabled()) {
            var serviceAccount = KeycloakSessionUtil.getKeycloakSession().users().getServiceAccount(model);
            if (serviceAccount != null) {
                return serviceAccount.getRoleMappingsStream()
                        .map(RoleModel::getName)
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    @Override
    public OIDCClientRepresentation createRepresentation() {
        return new OIDCClientRepresentation();
    }
}
