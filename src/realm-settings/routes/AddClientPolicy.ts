import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientPolicyParams = { realm: string };

export const AddClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/policies/add-client-policy",
  component: lazy(() => import("../NewClientPolicyForm")),
  breadcrumb: (t) => t("realm-settings:createPolicy"),
  access: "manage-clients",
};

export const toAddClientPolicy = (
  params: AddClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddClientPolicyRoute.path, params),
});
