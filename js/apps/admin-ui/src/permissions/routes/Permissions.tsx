import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionsParams = { realm: string };

const PermissionsSection = lazy(() => import("../PermissionsSection"));

export const PermissionsRoute: AppRouteObject = {
  path: "/:realm/permissions",
  element: <PermissionsSection />,
  breadcrumb: (t) => t("titlePermissions"),
  handle: {
    access: ["view-realm", "view-clients", "view-users"],
  },
};

export const toPermissions = (params: PermissionsParams): Partial<Path> => ({
  pathname: generateEncodedPath(PermissionsRoute.path, params),
});
