import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type EditClientPolicyParams = {
  realm: string;
  policyName: string;
};

const NewClientPolicy = lazy(() => import("../NewClientPolicy"));

export const EditClientPolicyRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy",
  element: <NewClientPolicy />,
  breadcrumb: (t) => t("policyDetails"),
  handle: {
    access: "manage-realm",
  },
};

export const toEditClientPolicy = (
  params: EditClientPolicyParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(EditClientPolicyRoute.path, params),
});
