import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderCreateParams = {
  realm: string;
  providerId: string;
};

const AddIdentityProvider = lazy(() => import("../add/AddIdentityProvider"));

export const IdentityProviderCreateRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/add",
  element: <AddIdentityProvider />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addProvider"),
  },
};

export const toIdentityProviderCreate = (
  params: IdentityProviderCreateParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderCreateRoute.path, params),
});
