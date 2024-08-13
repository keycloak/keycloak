import { lazy } from "react";
import { generatePath, type Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewPolicyParams = { realm: string; id: string; policyType: string };

const PolicyDetails = lazy(
  () => import("../authorization/policy/PolicyDetails"),
);

export const NewPolicyRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/policy/new/:policyType",
  element: <PolicyDetails />,
  breadcrumb: (t) => t("createPolicy"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("manage-clients", "manage-authorization"),
  },
};

export const toCreatePolicy = (params: NewPolicyParams): Partial<Path> => ({
  pathname: generatePath(NewPolicyRoute.path, params),
});
