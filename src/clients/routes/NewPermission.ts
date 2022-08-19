import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

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
  legacy: true,
};

export const toNewPermission = (
  params: NewPermissionParams
): Partial<Path> => ({
  pathname: generatePath(NewPermissionRoute.path, params),
});
