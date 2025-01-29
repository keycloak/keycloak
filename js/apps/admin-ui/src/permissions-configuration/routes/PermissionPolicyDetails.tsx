import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionPolicyDetailsParams = {
  realm: string;
  permissionClientId: string;
  policyId: string;
  resourceType: string;
};

const PermissionPolicyDetails = lazy(
  () => import("../permission-configuration/PermissionPolicyDetails"),
);

export const PermissionPolicyDetailsRoute: AppRouteObject = {
  path: "/:realm/permissions/:permissionClientId/policy/:policyId/:resourceType",
  element: <PermissionPolicyDetails />,
  breadcrumb: (t) => t("policyDetails"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny(
        "manage-clients",
        "view-authorization",
        "manage-authorization",
      ),
  },
};

export const toPermissionPolicyDetails = (
  params: PermissionPolicyDetailsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(PermissionPolicyDetailsRoute.path, params),
});
