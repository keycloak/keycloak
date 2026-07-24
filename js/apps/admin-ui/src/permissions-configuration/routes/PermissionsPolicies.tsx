import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionsPoliciesParams = {
  realm: string;
  permissionClientId: string;
};

const PermissionsPoliciesSection = lazy(
  () => import("../PermissionsConfigurationSection"),
);

export const PermissionsPoliciesRoute: AppRouteObject = {
  path: "/:realm/permissions/:permissionClientId/policies",
  element: <PermissionsPoliciesSection />,
  handle: {
    access: ["view-realm", "view-clients", "view-users"],
    breadcrumb: (t) => t("policies"),
  },
};

export const toPermissionsPolicies = (
  params: PermissionsPoliciesParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(PermissionsPoliciesRoute.path, params),
});
