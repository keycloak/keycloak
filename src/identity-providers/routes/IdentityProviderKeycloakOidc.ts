import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderKeycloakOidcParams = { realm: string };

export const IdentityProviderKeycloakOidcRoute: RouteDef = {
  path: "/:realm/identity-providers/keycloak-oidc/add",
  component: lazy(() => import("../add/AddOpenIdConnect")),
  breadcrumb: (t) => t("identity-providers:addKeycloakOpenIdProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderKeycloakOidc = (
  params: IdentityProviderKeycloakOidcParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderKeycloakOidcRoute.path, params),
});
