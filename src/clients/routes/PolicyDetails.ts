import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type PolicyDetailsParams = {
  realm: string;
  id: string;
  policyId: string;
  policyType: string;
};

export const PolicyDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/policy/:policyId/:policyType",
  component: lazy(() => import("../authorization/policy/PolicyDetails")),
  breadcrumb: (t) => t("clients:createPolicy"),
  access: "manage-clients",
};

export const toPolicyDetails = (
  params: PolicyDetailsParams
): LocationDescriptorObject => ({
  pathname: generatePath(PolicyDetailsRoute.path, params),
});
