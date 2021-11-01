import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import NewClientPolicyCondition from "../NewClientPolicyCondition";

export type EditClientPolicyConditionParams = {
  realm: string;
  policyName?: string;
  conditionName: string;
};

export const EditClientPolicyConditionRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:policyName?/edit-policy/:conditionName/edit-condition",
  component: NewClientPolicyCondition,
  breadcrumb: (t) => t("realm-settings:addCondition"),
  access: "manage-clients",
};

export const toEditClientPolicyCondition = (
  params: EditClientPolicyConditionParams
): LocationDescriptorObject => ({
  pathname: generatePath(EditClientPolicyConditionRoute.path, params),
});
