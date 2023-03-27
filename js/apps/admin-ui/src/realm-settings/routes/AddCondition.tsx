import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewClientPolicyConditionParams = {
  realm: string;
  policyName: string;
};

const NewClientPolicyCondition = lazy(
  () => import("../NewClientPolicyCondition")
);

export const NewClientPolicyConditionRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:policyName/edit-policy/create-condition",
  element: <NewClientPolicyCondition />,
  breadcrumb: (t) => t("realm-settings:addCondition"),
  handle: {
    access: "manage-clients",
  },
};

export const toNewClientPolicyCondition = (
  params: NewClientPolicyConditionParams
): Partial<Path> => ({
  pathname: generatePath(NewClientPolicyConditionRoute.path, params),
});
