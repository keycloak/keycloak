import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EditClientPolicyParams = {
  realm: string;
  policyName: string;
};

export const EditClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy",
  component: lazy(() => import("../NewClientPolicyForm")),
  access: "manage-realm",
  breadcrumb: (t) => t("realm-settings:policyDetails"),
};

export const toEditClientPolicy = (
  params: EditClientPolicyParams
): Partial<Path> => ({
  pathname: generatePath(EditClientPolicyRoute.path, params),
});
