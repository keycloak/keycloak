import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientPolicyCondition } from "../NewClientPolicyCondition";

export type NewClientPolicyConditionParams = {
  realm: string;
  policyName?: string;
};

export const NewClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:policyName?/edit-policy/create-condition",
  component: NewClientPolicyCondition,
  breadcrumb: (t) => t("realm-settings:addCondition"),
  access: "manage-clients",
};

export const toNewClientPolicyCondition = (
  params: NewClientPolicyConditionParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewClientPolicyConditionRoute.path, params),
});
