import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderSamlParams = { realm: string };

const AddSamlConnect = lazy(() => import("../add/AddSamlConnect"));

export const IdentityProviderSamlRoute: AppRouteObject = {
  path: "/:realm/identity-providers/saml/add",
  element: <AddSamlConnect />,
  breadcrumb: (t) => t("addSamlProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderSaml = (
  params: IdentityProviderSamlParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderSamlRoute.path, params),
});
