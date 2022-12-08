import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type IdentityProviderTab = "settings" | "mappers" | "permissions";

export type IdentityProviderParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: IdentityProviderTab;
};

export const IdentityProviderRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab",
  component: lazy(() => import("../add/DetailSettings")),
  breadcrumb: (t) => t("identity-providers:providerDetails"),
  access: "view-identity-providers",
};

export const toIdentityProvider = (
  params: IdentityProviderParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderRoute.path, params),
});
