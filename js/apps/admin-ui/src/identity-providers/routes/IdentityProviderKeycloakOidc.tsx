import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderKeycloakOidcParams = { realm: string };

const AddOpenIdConnect = lazy(() => import("../add/AddOpenIdConnect"));

export const IdentityProviderKeycloakOidcRoute: AppRouteObject = {
  path: "/:realm/identity-providers/keycloak-oidc/add",
  element: <AddOpenIdConnect />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addKeycloakOpenIdProvider"),
  },
};

export const toIdentityProviderKeycloakOidc = (
  params: IdentityProviderKeycloakOidcParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderKeycloakOidcRoute.path, params),
});
