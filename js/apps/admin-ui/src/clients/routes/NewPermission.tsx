import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type PermissionType = "resource" | "scope";

export type NewPermissionParams = {
  realm: string;
  id: string;
  permissionType: PermissionType;
  selectedId?: string;
};

const PermissionDetails = lazy(
  () => import("../authorization/PermissionDetails"),
);

export const NewPermissionRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/permission/new/:permissionType",
  element: <PermissionDetails />,
  breadcrumb: (t) => t("createPermission"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("manage-clients", "manage-authorization"),
  },
};

export const NewPermissionWithSelectedIdRoute: AppRouteObject = {
  ...NewPermissionRoute,
  path: "/:realm/clients/:id/authorization/permission/new/:permissionType/:selectedId",
};

export const toNewPermission = (params: NewPermissionParams): Partial<Path> => {
  const path = params.selectedId
    ? NewPermissionWithSelectedIdRoute.path
    : NewPermissionRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
