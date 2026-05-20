package org.keycloak.models.mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation.Auth;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OIDCClientModelMapper extends BaseClientModelMapper<OIDCClientRepresentation> {
    @Override
    protected OIDCClientRepresentation createClientRepresentation() {
        return new OIDCClientRepresentation();
    }
    
    public OIDCClientModelMapper() {
        addMapping("loginFlows", OIDCClientRepresentation::getLoginFlows, OIDCClientRepresentation::setLoginFlows, model -> createLoginFlows(model), (model, flows) -> setModelFromFlows(flows, model));
        addMapping("auth", OIDCClientRepresentation::getAuth, OIDCClientRepresentation::setAuth, model -> getAuth(model), (model, auth) -> setAuth(model, auth));
        addMapping("webOrigins", OIDCClientRepresentation::getWebOrigins, OIDCClientRepresentation::setWebOrigins, model -> new LinkedHashSet<>(model.getWebOrigins()), (model, webOrigins) -> model.setWebOrigins(new LinkedHashSet<>(webOrigins)));
        addMapping("serviceAccountRoles", OIDCClientRepresentation::getServiceAccountRoles, OIDCClientRepresentation::setServiceAccountRoles, model -> getServiceAccountRoles(model), null);
    }

    private OIDCClientRepresentation.Auth getAuth(ClientModel model) {
        OIDCClientRepresentation.Auth auth = null;
        if (!model.isPublicClient()) {
            auth = new OIDCClientRepresentation.Auth();
            auth.setMethod(model.getClientAuthenticatorType());
            auth.setSecret(model.getSecret());
            // TODO: auth.certificate
        }
        return auth;
    }

    private void setAuth(ClientModel model, Auth auth) {
        if (auth != null) {
            model.setPublicClient(false);
            model.setClientAuthenticatorType(auth.getMethod());
            model.setSecret(auth.getSecret());
        } else {
            model.setPublicClient(true);
        }
    }

    private Set<OIDCClientRepresentation.Flow> createLoginFlows(ClientModel model) {
        Set<OIDCClientRepresentation.Flow> flows = new HashSet<>();
        if (model.isStandardFlowEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.STANDARD);
        }
        if (model.isImplicitFlowEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.IMPLICIT);
        }
        if (model.isDirectAccessGrantsEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.DIRECT_GRANT);
        }
        // TODO: device flow, token exchange, ciba
        if (model.isServiceAccountsEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);
        }
        return flows;
    }

    private void setModelFromFlows(Set<OIDCClientRepresentation.Flow> flows, ClientModel model) {
        model.setStandardFlowEnabled(flows.contains(OIDCClientRepresentation.Flow.STANDARD));
        model.setImplicitFlowEnabled(flows.contains(OIDCClientRepresentation.Flow.IMPLICIT));
        model.setDirectAccessGrantsEnabled(flows.contains(OIDCClientRepresentation.Flow.DIRECT_GRANT));
    }

    private Set<String> getServiceAccountRoles(ClientModel client) {
        if (client.isServiceAccountsEnabled()) {
            var serviceAccount = KeycloakSessionUtil.getKeycloakSession().users().getServiceAccount(client);
            if (serviceAccount != null) {
                return serviceAccount.getRoleMappingsStream()
                        .map(RoleModel::getName)
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }
}
