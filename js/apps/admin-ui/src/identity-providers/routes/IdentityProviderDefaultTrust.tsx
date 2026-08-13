import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderDefaultTrustParams = { realm: string };

const AddDefaultTrust = lazy(() => import("../add/AddDefaultTrust"));

export const IdentityProviderDefaultTrustRoute: AppRouteObject = {
  path: "/:realm/identity-providers/default-trust/add",
  element: <AddDefaultTrust />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addProvider"),
  },
};

export const toIdentityProviderDefaultTrust = (
  params: IdentityProviderDefaultTrustParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderDefaultTrustRoute.path, params),
});
