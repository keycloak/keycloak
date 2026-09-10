import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderOid4VpParams = { realm: string };

const AddOid4Vp = lazy(() => import("../add/AddOid4Vp"));

export const IdentityProviderOid4VpRoute: AppRouteObject = {
  path: "/:realm/identity-providers/oid4vp/add",
  element: <AddOid4Vp />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addProvider"),
  },
};

export const toIdentityProviderOid4Vp = (
  params: IdentityProviderOid4VpParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderOid4VpRoute.path, params),
});
