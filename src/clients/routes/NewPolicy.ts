import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type NewPolicyParams = { realm: string; id: string; policyType: string };

export const NewPolicyRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/policy/new/:policyType",
  component: lazy(() => import("../authorization/policy/PolicyDetails")),
  breadcrumb: (t) => t("clients:createPolicy"),
  access: "manage-clients",
};

export const toCreatePolicy = (
  params: NewPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewPolicyRoute.path, params),
});
