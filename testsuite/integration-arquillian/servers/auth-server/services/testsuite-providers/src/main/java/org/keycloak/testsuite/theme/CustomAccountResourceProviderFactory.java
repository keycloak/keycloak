package org.keycloak.testsuite.theme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resource.AccountResourceProviderFactory;

import org.jboss.resteasy.reactive.NoCache;

public class CustomAccountResourceProviderFactory implements AccountResourceProviderFactory, AccountResourceProvider {
  public static final String ID = "ext-custom-account-console";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public AccountResourceProvider create(KeycloakSession session) {
    return this;
  }

  @Override
  public Object getResource() {
    return this;
  }

  @GET
  @NoCache
  @Produces(MediaType.TEXT_HTML)
  public Response getMainPage() {
    return Response.ok().entity("<html><head><title>Account</title></head><body><h1>Custom Account Console</h1></body></html>").build();
  }
  
  @Override
  public void init(Scope config) {}

  @Override
  public void postInit(KeycloakSessionFactory factory) {}

  @Override
  public void close() {}
}
