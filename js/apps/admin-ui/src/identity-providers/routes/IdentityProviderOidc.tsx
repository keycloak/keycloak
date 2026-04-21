import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderOidcParams = { realm: string };

const AddOpenIdConnect = lazy(() => import("../add/AddOpenIdConnect"));

export const IdentityProviderOidcRoute: AppRouteObject = {
  path: "/:realm/identity-providers/oidc/add",
  element: <AddOpenIdConnect />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addOpenIdProvider"),
  },
};

export const toIdentityProviderOidc = (
  params: IdentityProviderOidcParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderOidcRoute.path, params),
});
