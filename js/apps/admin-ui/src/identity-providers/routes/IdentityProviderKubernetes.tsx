import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderKubernetesParams = { realm: string };

const AddKubernetesConnect = lazy(() => import("../add/AddKubernetesConnect"));

export const IdentityProviderKubernetesRoute: AppRouteObject = {
  path: "/:realm/identity-providers/kubernetes/add",
  element: <AddKubernetesConnect />,
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addKubernetesProvider"),
  },
};

export const toIdentityProviderKubernetes = (
  params: IdentityProviderKubernetesParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderKubernetesRoute.path, params),
});
