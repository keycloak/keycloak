package org.keycloak.protocol.oid4vc.clientpolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientModelContext;

public class CredentialOfferPolicyContext implements ClientModelContext {

    private final ClientPolicyEvent event;
    private final CredentialScopeModel credScopeModel;
    private final CredentialOfferState offerState;
    private final ClientModel clientModel;
    private final List<String> evaluatedBy = new ArrayList<>();

    public CredentialOfferPolicyContext(ClientPolicyEvent event, ClientModel clientModel, CredentialScopeModel credScopeModel, CredentialOfferState offerState) {
        this.event = event;
        this.clientModel = clientModel;
        this.credScopeModel = credScopeModel;
        this.offerState = offerState;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return event;
    }

    public CredentialScopeModel getCredentialScopeModel() {
        return credScopeModel;
    }

    public CredentialOfferState getCredentialOfferState() {
        return offerState;
    }

    @Override
    public ClientModel getClient() {
        return clientModel;
    }

    public List<String> getEvaluatedBy() {
        return Collections.unmodifiableList(evaluatedBy);
    }

    public void setEvaluatedBy(String providerId) {
        evaluatedBy.add(providerId);
    }

    public boolean wasEvaluatedBy(String providerId) {
        return evaluatedBy.contains(providerId);
    }
}
