import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PolicyDetailsParams = {
  realm: string;
  id: string;
  policyId: string;
  policyType: string;
};

const PolicyDetails = lazy(
  () => import("../authorization/policy/PolicyDetails"),
);

export const PolicyDetailsRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/policy/:policyId/:policyType",
  element: <PolicyDetails />,
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny(
        "manage-clients",
        "view-authorization",
        "manage-authorization",
      ),
    breadcrumb: (t) => t("policyDetails"),
  },
};

export const toPolicyDetails = (
  params: PolicyDetailsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(PolicyDetailsRoute.path, params),
});
