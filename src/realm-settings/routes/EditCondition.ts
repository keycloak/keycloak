import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EditClientPolicyConditionParams = {
  realm: string;
  policyName?: string;
  conditionName: string;
};

export const EditClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:policyName?/edit-policy/:conditionName/edit-condition",
  component: lazy(() => import("../NewClientPolicyCondition")),
  breadcrumb: (t) => t("realm-settings:editCondition"),
  access: "manage-clients",
  legacy: true,
};

export const toEditClientPolicyCondition = (
  params: EditClientPolicyConditionParams
): LocationDescriptorObject => ({
  pathname: generatePath(EditClientPolicyConditionRoute.path, params),
});
