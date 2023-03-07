import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

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
  access: "view-clients",
};

export const toPolicyDetails = (
  params: PolicyDetailsParams
): Partial<Path> => ({
  pathname: generatePath(PolicyDetailsRoute.path, params),
});
