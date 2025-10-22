import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderEntraIdOidcParams = { realm: string };

const AddOpenIdConnect = lazy(() => import("../add/AddOpenIdConnect"));

export const IdentityProviderEntraIdOidcRoute: AppRouteObject = {
  path: "/:realm/identity-providers/entraid-oidc/add",
  element: <AddOpenIdConnect />,
  breadcrumb: (t) => t("addEntraIdOpenIdProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderEntraIdOidc = (
  params: IdentityProviderEntraIdOidcParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderEntraIdOidcRoute.path, params),
});
