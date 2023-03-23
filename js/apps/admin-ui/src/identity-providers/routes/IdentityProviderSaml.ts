import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderSamlParams = { realm: string };

export const IdentityProviderSamlRoute: RouteDef = {
  path: "/:realm/identity-providers/saml/add",
  component: lazy(() => import("../add/AddSamlConnect")),
  breadcrumb: (t) => t("identity-providers:addSamlProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderSaml = (
  params: IdentityProviderSamlParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderSamlRoute.path, params),
});
