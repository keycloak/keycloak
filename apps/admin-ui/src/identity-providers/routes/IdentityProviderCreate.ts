import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderCreateParams = {
  realm: string;
  providerId: string;
};

export const IdentityProviderCreateRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/add",
  component: lazy(() => import("../add/AddIdentityProvider")),
  breadcrumb: (t) => t("identity-providers:addProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderCreate = (
  params: IdentityProviderCreateParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderCreateRoute.path, params),
});
