package org.keycloak.protocol.oid4vc.clientpolicy;

import org.keycloak.models.ClientModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class CredentialOfferPolicyContext implements ClientPolicyContext {

    private final ClientPolicyEvent event;
    private CredentialScopeModel credScopeModel;
    private CredentialOfferState offerState;
    private ClientModel clientModel;
    private boolean evaluatedOnEvent;

    public CredentialOfferPolicyContext(ClientPolicyEvent event) {
        this.event = event;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return event;
    }

    public CredentialScopeModel getCredentialScopeModel() {
        return credScopeModel;
    }

    public CredentialOfferPolicyContext setCredentialScopeModel(CredentialScopeModel credScopeModel) {
        this.credScopeModel = credScopeModel;
        return this;
    }

    public CredentialOfferState getCredentialOfferState() {
        return offerState;
    }

    public CredentialOfferPolicyContext setCredentialOfferState(CredentialOfferState offerState) {
        this.offerState = offerState;
        return this;
    }

    public ClientModel getClientModel() {
        return clientModel;
    }

    public CredentialOfferPolicyContext setClientModel(ClientModel clientModel) {
        this.clientModel = clientModel;
        return this;
    }

    public boolean isEvaluatedOnEvent() {
        return evaluatedOnEvent;
    }

    public void setEvaluatedOnEvent(boolean evaluated) {
        this.evaluatedOnEvent = evaluated;
    }
}
