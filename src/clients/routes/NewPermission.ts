import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type PermissionType = "resource" | "scope";

export type NewPermissionParams = {
  realm: string;
  id: string;
  permissionType: PermissionType;
  selectedId?: string;
};

export const NewPermissionRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/permission/new/:permissionType/:selectedId?",
  component: lazy(() => import("../authorization/PermissionDetails")),
  breadcrumb: (t) => t("clients:createPermission"),
  access: "view-clients",
};

export const toNewPermission = (
  params: NewPermissionParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewPermissionRoute.path, params),
});
