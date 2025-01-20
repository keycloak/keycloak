import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type NewPermissionPolicyDetailsParams = {
  realm: string;
  permissionClientId: string;
};

const NewPermissionPolicyDetails = lazy(
  () => import("../permission-configuration/PermissionPolicyDetails"),
);

export const NewPermissionPolicyRoute: AppRouteObject = {
  path: "/:realm/permissions/:permissionClientId/policy/new",
  element: <NewPermissionPolicyDetails />,
  breadcrumb: (t) => t("createPolicy"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("manage-clients", "manage-authorization"),
  },
};

export const toCreatePermissionPolicy = (
  params: NewPermissionPolicyDetailsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(NewPermissionPolicyRoute.path, params),
});
