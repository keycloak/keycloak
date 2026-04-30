package org.keycloak.protocol.oid4vc.clientpolicy;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class CredentialClientPolicyContext implements ClientPolicyContext {

    private final ClientPolicyEvent event;
    private CredentialScopeModel credScopeModel;
    private CredentialOfferState offerState;
    private boolean evaluatedOnEvent;

    public CredentialClientPolicyContext(ClientPolicyEvent event) {
        this.event = event;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return event;
    }

    public CredentialScopeModel getCredentialScopeModel() {
        return credScopeModel;
    }

    public CredentialClientPolicyContext setCredentialScopeModel(CredentialScopeModel credScopeModel) {
        this.credScopeModel = credScopeModel;
        return this;
    }

    public CredentialOfferState getCredentialOfferState() {
        return offerState;
    }

    public CredentialClientPolicyContext setCredentialOfferState(CredentialOfferState offerState) {
        this.offerState = offerState;
        return this;
    }

    public boolean isEvaluatedOnEvent() {
        return evaluatedOnEvent;
    }

    public void setEvaluatedOnEvent(boolean evaluated) {
        this.evaluatedOnEvent = evaluated;
    }
}
