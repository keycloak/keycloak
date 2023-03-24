import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderKeycloakOidcParams = { realm: string };

const AddOpenIdConnect = lazy(() => import("../add/AddOpenIdConnect"));

export const IdentityProviderKeycloakOidcRoute: RouteDef = {
  path: "/:realm/identity-providers/keycloak-oidc/add",
  element: <AddOpenIdConnect />,
  breadcrumb: (t) => t("identity-providers:addKeycloakOpenIdProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderKeycloakOidc = (
  params: IdentityProviderKeycloakOidcParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderKeycloakOidcRoute.path, params),
});
