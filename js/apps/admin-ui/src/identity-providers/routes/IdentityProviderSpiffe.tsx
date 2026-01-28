import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderSpiffeParams = { realm: string };

const AddSpiffeConnect = lazy(() => import("../add/AddSpiffeConnect"));

export const IdentityProviderSpiffeRoute: AppRouteObject = {
  path: "/:realm/identity-providers/spiffe/add",
  element: <AddSpiffeConnect />,
  breadcrumb: (t) => t("addSpiffeProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderSpiffe = (
  params: IdentityProviderSpiffeParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderSpiffeRoute.path, params),
});
