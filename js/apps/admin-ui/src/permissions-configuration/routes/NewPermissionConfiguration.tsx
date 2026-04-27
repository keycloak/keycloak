import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type NewPermissionConfigurationParams = {
  realm: string;
  permissionClientId: string;
  resourceType: string;
};

const PermissionConfigurationDetails = lazy(
  () => import("../permission-configuration/PermissionConfigurationDetails"),
);

export const NewPermissionConfigurationRoute: AppRouteObject = {
  path: "/:realm/permissions/:permissionClientId/permission/new/:resourceType",
  element: <PermissionConfigurationDetails />,
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("manage-clients", "manage-authorization"),
    breadcrumb: (t) => t("createPermission"),
  },
};

export const toCreatePermissionConfiguration = (
  params: NewPermissionConfigurationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(NewPermissionConfigurationRoute.path, params),
});
