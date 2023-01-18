import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientPolicyParams = { realm: string };

export const AddClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/policies/add-client-policy",
  component: lazy(() => import("../NewClientPolicyForm")),
  breadcrumb: (t) => t("realm-settings:createPolicy"),
  access: "manage-clients",
};

export const toAddClientPolicy = (
  params: AddClientPolicyParams
): Partial<Path> => ({
  pathname: generatePath(AddClientPolicyRoute.path, params),
});
