package org.keycloak.admin.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

@Provider
public class DefaultAdminRootV2 implements AdminRootV2 {

  @Context
  protected KeycloakSession session;

  @Override
  public AdminApi adminApi(String realmName) {
    checkApiEnabled();
    return new DefaultAdminApi(session, realmName);
  }

  @Override
  public Response preFlight() {
    checkApiEnabled();
    return new AdminCorsPreflightService().preflight();
  }

  private void checkApiEnabled() {
    if (!isAdminApiV2Enabled()) {
      throw new NotFoundException();
    }
  }

  public static boolean isAdminApiV2Enabled() {
    return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
  }
}
