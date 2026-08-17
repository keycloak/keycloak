package org.keycloak.services.client.scim;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    private OIDCClientModelSchema() {
    }

    @Override
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, OIDCClientRepresentation>> map) {
        map.put("loginFlows",          multivaluedStringAttr("loginFlows",          (rep, v) -> rep.setLoginFlows(toFlowSet(v)), (BiConsumer<ClientModel, Set<OIDCClientRepresentation.Flow>>) (model, flows) -> setModelFromFlows(flows, model)));
        map.put("auth",                customAttr           ("auth",                OIDCClientRepresentation::setAuth,           (model, auth) -> setAuth(model, (OIDCClientRepresentation.Auth) auth)));
        map.put("webOrigins",          multivaluedStringAttr("webOrigins",          (rep, v) -> rep.setWebOrigins(v),            (BiConsumer<ClientModel, Set<String>>) (model, origins) -> model.setWebOrigins(origins != null ? new LinkedHashSet<>(origins) : null)));
        map.put("serviceAccountRoles", multivaluedStringAttr("serviceAccountRoles", (rep, v) -> rep.setServiceAccountRoles(v),  null));
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
