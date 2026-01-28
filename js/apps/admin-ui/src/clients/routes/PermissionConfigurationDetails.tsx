import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionConfigurationDetailParams = {
  realm: string;
  id: string;
  permissionId: string;
  permissionType: string;
};

const PermissionConfigurationDetails = lazy(
  () =>
    import(
      "../../permissions-configuration/permission-configuration/PermissionConfigurationDetails"
    ),
);

export const PermissionConfigurationDetailRoute: AppRouteObject = {
  path: "/:realm/clients/:id/permissions/permission/:permissionId/:permissionType",
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
  params: PermissionConfigurationDetailParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    PermissionConfigurationDetailRoute.path,
    params,
  ),
});
