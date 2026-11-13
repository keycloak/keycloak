import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderEtsiTrustListParams = { realm: string };

const AddEtsiTrustList = lazy(() => import("../add/AddEtsiTrustList"));

export const IdentityProviderEtsiTrustListRoute: AppRouteObject = {
  path: "/:realm/identity-providers/etsi-trust-list/add",
  element: <AddEtsiTrustList />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addProvider"),
  },
};

export const toIdentityProviderEtsiTrustList = (
  params: IdentityProviderEtsiTrustListParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    IdentityProviderEtsiTrustListRoute.path,
    params,
  ),
});
