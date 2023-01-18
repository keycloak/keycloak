import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserTab = "list" | "permissions";

export type UsersParams = { realm: string; tab?: UserTab };

export const UsersRoute: RouteDef = {
  path: "/:realm/users",
  component: lazy(() => import("../UsersSection")),
  breadcrumb: (t) => t("users:title"),
  access: "query-users",
};

export const UsersRouteWithTab: RouteDef = {
  ...UsersRoute,
  path: "/:realm/users/:tab",
};

export const toUsers = (params: UsersParams): Partial<Path> => {
  const path = params.tab ? UsersRouteWithTab.path : UsersRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
