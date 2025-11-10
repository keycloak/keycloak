package org.keycloak.verifiableclaims.defaultimpl;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.verifiableclaims.VerifiableClaimProvider;
import org.keycloak.verifiableclaims.VerifiableClaimProviderFactory;

public class DefaultVerifiableClaimProviderFactory implements VerifiableClaimProviderFactory {
    @Override public VerifiableClaimProvider create(KeycloakSession session) { return new DefaultVerifiableClaimProvider(session); }
    @Override public void init(Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }
    @Override public String getId() { return "default-verifiable-claims"; }
}
