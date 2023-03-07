import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderOidcParams = { realm: string };

export const IdentityProviderOidcRoute: RouteDef = {
  path: "/:realm/identity-providers/oidc/add",
  component: lazy(() => import("../add/AddOpenIdConnect")),
  breadcrumb: (t) => t("identity-providers:addOpenIdProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderOidc = (
  params: IdentityProviderOidcParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderOidcRoute.path, params),
});
