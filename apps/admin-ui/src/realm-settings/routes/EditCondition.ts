import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type EditClientPolicyConditionParams = {
  realm: string;
  policyName?: string;
  conditionName?: string;
};

export const EditClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies",
  component: lazy(() => import("../NewClientPolicyCondition")),
  breadcrumb: (t) => t("realm-settings:editCondition"),
  access: "manage-clients",
};

export const EditClientPolicyConditionWithPolicyNameRoute: RouteDef = {
  ...EditClientPolicyConditionRoute,
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy/:conditionName/edit-condition",
};

export const toEditClientPolicyCondition = (
  params: EditClientPolicyConditionParams
): Partial<Path> => {
  const path = params.policyName
    ? EditClientPolicyConditionWithPolicyNameRoute.path
    : EditClientPolicyConditionRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
