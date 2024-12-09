import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionsTabs = "resources" | "policies" | "evaluate";

export type PermissionsTabsParams = {
  realm: string;
  tab: PermissionsTabs;
};

const PermissionsSection = lazy(() => import("../PermissionsSection"));

export const PermissionsTabsRoute: AppRouteObject = {
  path: "/:realm/permissions/:tab",
  element: <PermissionsSection />,
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("view-realm", "view-clients", "view-users"),
  },
};

export const toPermissionsTabs = (
  params: PermissionsTabsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(PermissionsTabsRoute.path, params),
});
