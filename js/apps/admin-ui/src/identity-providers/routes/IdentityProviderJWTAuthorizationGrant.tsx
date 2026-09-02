import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderJWTAuthorizationGrantParams = { realm: string };

const AddJWTAuthorizationGrant = lazy(
  () => import("../add/AddJWTAuthorizationGrant"),
);

export const IdentityProviderJWTAuthorizationGrantRoute: AppRouteObject = {
  path: "/:realm/identity-providers/jwt-authorization-grant/add",
  element: <AddJWTAuthorizationGrant />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addJWTAuthorizationGrantProvider"),
  },
};

export const toIdentityProviderJWTAuthorizationGrant = (
  params: IdentityProviderJWTAuthorizationGrantParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    IdentityProviderJWTAuthorizationGrantRoute.path,
    params,
  ),
});
