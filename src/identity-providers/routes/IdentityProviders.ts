import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProvidersParams = { realm: string };

export const IdentityProvidersRoute: RouteDef = {
  path: "/:realm/identity-providers",
  component: lazy(() => import("../IdentityProvidersSection")),
  breadcrumb: (t) => t("identityProviders"),
  access: "view-identity-providers",
};

export const toIdentityProviders = (
  params: IdentityProvidersParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProvidersRoute.path, params),
});
