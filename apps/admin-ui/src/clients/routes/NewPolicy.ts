import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewPolicyParams = { realm: string; id: string; policyType: string };

export const NewPolicyRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/policy/new/:policyType",
  component: lazy(() => import("../authorization/policy/PolicyDetails")),
  breadcrumb: (t) => t("clients:createPolicy"),
  access: "view-clients",
};

export const toCreatePolicy = (params: NewPolicyParams): Partial<Path> => ({
  pathname: generatePath(NewPolicyRoute.path, params),
});
