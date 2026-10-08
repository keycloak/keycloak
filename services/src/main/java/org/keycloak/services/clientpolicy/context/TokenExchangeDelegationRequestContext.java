package org.keycloak.services.clientpolicy.context;

import java.util.Collections;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.tokenexchange.DelegationActor;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * Fired by the token exchange delegation provider once both the subject and the actor token are verified, but before
 * the delegation itself is authorized. Executors can inspect the shape of the delegation and decide whether the
 * chain may continue.
 */
public class TokenExchangeDelegationRequestContext implements ClientModelContext, ScopeParameterContext {

    private final TokenExchangeContext tokenExchangeContext;
    private final boolean clientDelegation;
    private final boolean mayActPresent;
    private final List<DelegationActor> priorActors;

    private Boolean chainingAllowed;

    public TokenExchangeDelegationRequestContext(TokenExchangeContext tokenExchangeContext,
            boolean clientDelegation, boolean mayActPresent, List<DelegationActor> priorActors) {
        this.tokenExchangeContext = tokenExchangeContext;
        this.clientDelegation = clientDelegation;
        this.mayActPresent = mayActPresent;
        this.priorActors = Collections.unmodifiableList(priorActors);
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.TOKEN_EXCHANGE_DELEGATION_REQUEST;
    }

    @Override
    public ClientModel getClient() {
        return tokenExchangeContext.getClient();
    }

    @Override
    public String getScopeParameter() {
        return tokenExchangeContext.getParams().getScope();
    }

    /**
     * True when the actor is a service account, so this hop is client delegation rather than admin delegation.
     */
    public boolean isClientDelegation() {
        return clientDelegation;
    }

    public boolean isMayActPresent() {
        return mayActPresent;
    }

    /**
     * The actors already recorded in the "act" claim this hop inherits, most recent first, as defined by RFC 8693
     * section 4.1. The delegated token nests them below the actor of this request.
     */
    public List<DelegationActor> getPriorActors() {
        return priorActors;
    }

    /**
     * Number of actors the resulting "act" claim would carry, including the actor of this request.
     */
    public int getChainDepth() {
        return priorActors.size() + 1;
    }

    /**
     * True when an already delegated token can be exchanged again. Off unless a policy turns it on.
     */
    public boolean isChainingAllowed() {
        return Boolean.TRUE.equals(chainingAllowed);
    }

    /**
     * Grants or denies chaining for this request, the token exchange delegation provider enforces the outcome.
     * Mutable because {@code triggerOnEvent} returns void, so an executor granting chaining has no other channel
     * back to the provider. Chaining is default-deny and the value folds with AND, so a permissive policy can never
     * override a restrictive one and the outcome does not depend on the order the policies are evaluated in.
     */
    public void restrictChainingAllowed(boolean allowed) {
        // a null field means no executor has spoken yet, so the fold starts from the first value, not from the default
        chainingAllowed = chainingAllowed == null ? allowed : chainingAllowed && allowed;
    }
}
