import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";
import type { PermissionType } from "./NewPermission";

export type PermissionDetailsParams = {
  realm: string;
  id: string;
  permissionType: string | PermissionType;
  permissionId: string;
};

export const PermissionDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/permission/:permissionType/:permissionId",
  component: lazy(() => import("../authorization/PermissionDetails")),
  breadcrumb: (t) => t("clients:permissionDetails"),
  access: "manage-clients",
};

export const toPermissionDetails = (
  params: PermissionDetailsParams
): LocationDescriptorObject => ({
  pathname: generatePath(PermissionDetailsRoute.path, params),
});
