import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProvidersParams = { realm: string };

const IdentityProvidersSection = lazy(
  () => import("../IdentityProvidersSection")
);

export const IdentityProvidersRoute: RouteDef = {
  path: "/:realm/identity-providers",
  element: <IdentityProvidersSection />,
  breadcrumb: (t) => t("identityProviders"),
  access: "view-identity-providers",
};

export const toIdentityProviders = (
  params: IdentityProvidersParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProvidersRoute.path, params),
});
