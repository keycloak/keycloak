import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderCreateParams = {
  realm: string;
  providerId: string;
};

const AddIdentityProvider = lazy(() => import("../add/AddIdentityProvider"));

export const IdentityProviderCreateRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/add",
  element: <AddIdentityProvider />,
  breadcrumb: (t) => t("identity-providers:addProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderCreate = (
  params: IdentityProviderCreateParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderCreateRoute.path, params),
});
