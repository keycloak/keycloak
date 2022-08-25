import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type NewClientPolicyConditionParams = {
  realm: string;
  policyName?: string;
};

export const NewClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies",
  component: lazy(() => import("../NewClientPolicyCondition")),
  breadcrumb: (t) => t("realm-settings:addCondition"),
  access: "manage-clients",
};

export const NewClientPolicyConditionWithPolicyNameRoute: RouteDef = {
  ...NewClientPolicyConditionRoute,
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy/create-condition",
};

export const toNewClientPolicyCondition = (
  params: NewClientPolicyConditionParams
): Partial<Path> => {
  const path = params.policyName
    ? NewClientPolicyConditionWithPolicyNameRoute.path
    : NewClientPolicyConditionRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
