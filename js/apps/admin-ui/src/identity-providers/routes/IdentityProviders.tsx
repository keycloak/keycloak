import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProvidersParams = { realm: string };

const IdentityProvidersSection = lazy(
  () => import("../IdentityProvidersSection"),
);

export const IdentityProvidersRoute: AppRouteObject = {
  path: "/:realm/identity-providers",
  element: <IdentityProvidersSection />,
  breadcrumb: (t) => t("identityProviders"),
  handle: {
    access: "view-identity-providers",
  },
};

export const toIdentityProviders = (
  params: IdentityProvidersParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProvidersRoute.path, params),
});
