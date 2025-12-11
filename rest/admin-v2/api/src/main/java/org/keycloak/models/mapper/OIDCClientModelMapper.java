package org.keycloak.models.mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OIDCClientModelMapper extends BaseClientModelMapper<OIDCClientRepresentation> {
    public OIDCClientModelMapper(KeycloakSession session) {
        super(session);
    }

    @Override
    protected OIDCClientRepresentation createClientRepresentation() {
        return new OIDCClientRepresentation();
    }

    @Override
    protected void fromModelSpecific(ClientModel model, OIDCClientRepresentation rep) {
        rep.setLoginFlows(createLoginFlows(model));

        if (!model.isPublicClient()) {
            OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
            auth.setMethod(model.getClientAuthenticatorType());
            auth.setSecret(model.getSecret());
            rep.setAuth(auth);
            // TODO: auth.certificate
        }

        rep.setWebOrigins(new HashSet<>(model.getWebOrigins()));
        rep.setServiceAccountRoles(getServiceAccountRoles(model));
    }

    @Override
    protected void toModelSpecific(OIDCClientRepresentation rep, ClientModel model) {
        if (rep.getAuth() != null) {
            model.setPublicClient(false);
            model.setClientAuthenticatorType(rep.getAuth().getMethod());
            model.setSecret(rep.getAuth().getSecret());
        } else {
            model.setPublicClient(true);
        }

        setModelFromFlows(rep.getLoginFlows(), model);

        model.setWebOrigins(new HashSet<>(rep.getWebOrigins()));

        // Service account roles are not handled here
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
            return session.users().getServiceAccount(client)
                    .getRoleMappingsStream()
                    .map(RoleModel::getName)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
