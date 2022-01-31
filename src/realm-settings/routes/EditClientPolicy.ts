import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EditClientPolicyParams = {
  realm: string;
  policyName: string;
};

export const EditClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:policyName/edit-policy",
  component: lazy(() => import("../NewClientPolicyForm")),
  access: "manage-realm",
  breadcrumb: (t) => t("realm-settings:policyDetails"),
};

export const toEditClientPolicy = (
  params: EditClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(EditClientPolicyRoute.path, params),
});
