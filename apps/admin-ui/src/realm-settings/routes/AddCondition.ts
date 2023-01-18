import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewClientPolicyConditionParams = {
  realm: string;
  policyName: string;
};

export const NewClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy/create-condition",
  component: lazy(() => import("../NewClientPolicyCondition")),
  breadcrumb: (t) => t("realm-settings:addCondition"),
  access: "manage-clients",
};

export const toNewClientPolicyCondition = (
  params: NewClientPolicyConditionParams
): Partial<Path> => ({
  pathname: generatePath(NewClientPolicyConditionRoute.path, params),
});
