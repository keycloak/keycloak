import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionConfigurationDetailsParams = {
  realm: string;
  permissionClientId: string;
  permissionId: string;
  resourceType: string;
};

const PermissionConfigurationDetails = lazy(
  () =>
    import(
      "../../permissions-configuration/permission-configuration/PermissionConfigurationDetails"
    ),
);

export const PermissionConfigurationDetailRoute: AppRouteObject = {
  path: "/:realm/permissions/:permissionClientId/permission/:permissionId/:resourceType",
  element: <PermissionConfigurationDetails />,
  breadcrumb: (t) => t("permissionDetails"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny(
        "manage-clients",
        "view-authorization",
        "manage-authorization",
      ),
  },
};

export const toPermissionConfigurationDetails = (
  params: PermissionConfigurationDetailsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    PermissionConfigurationDetailRoute.path,
    params,
  ),
});
