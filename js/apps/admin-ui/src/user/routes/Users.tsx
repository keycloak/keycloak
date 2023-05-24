import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type UserTab = "list" | "permissions";

export type UsersParams = { realm: string; tab?: UserTab };

const UsersSection = lazy(() => import("../UsersSection"));

export const UsersRoute: AppRouteObject = {
  path: "/:realm/users",
  element: <UsersSection />,
  breadcrumb: (t) => t("users:title"),
  handle: {
    access: "query-users",
  },
};

export const UsersRouteWithTab: AppRouteObject = {
  ...UsersRoute,
  path: "/:realm/users/:tab",
};

export const toUsers = (params: UsersParams): Partial<Path> => {
  const path = params.tab ? UsersRouteWithTab.path : UsersRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
